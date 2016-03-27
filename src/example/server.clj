(ns example.server
  "Official Sente reference example: server"
  {:author "Peter Taoussanis (@ptaoussanis)"}

  (:require
   [clojure.string     :as str]
   [ring.middleware.defaults]
   [ring.util.response :as response]
   [compojure.core     :as comp :refer (defroutes GET POST wrap-routes)]
   [compojure.route    :as route]
   [hiccup.core        :as hiccup]
   [hiccup.form        :as form]
   [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
   [taoensso.encore    :as encore :refer ()]
   [taoensso.timbre    :as timbre :refer (tracef debugf infof warnf errorf)]
   [taoensso.sente     :as sente]

   [org.httpkit.server :as http-kit]
   [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]
   ))

;;;; Define our Sente channel socket (chsk) server

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket-server! sente-web-server-adapter
        {:packer :edn})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

;;;; Ring handlers

(defn landing-pg-handler [ring-req]
  (hiccup/html
    [:h1 "Sente reference example"]
    [:p "An Ajax/WebSocket" [:strong " (random choice!)"] " has been configured for this example"]
    [:hr]
    [:p [:strong "Step 1: "] " try hitting the buttons:"]
    [:button#btn1 {:type "button"} "chsk-send! (w/o reply)"]
    [:button#btn2 {:type "button"} "chsk-send! (with reply)"]
    ;;
    [:p [:strong "Step 2: "] " observe std-out (for server output) and below (for client output):"]
    [:textarea#output {:style "width: 100%; height: 200px;"}]
    ;;
    [:hr]
    [:h2 "Step 3: try login with a user-id"]
    [:p  "The server can use this id to send events to *you* specifically."]
    [:p
     [:input#input-login {:type :text :placeholder "User-id"}]
     [:button#btn-login {:type "button"} "Secure login!"]]
    ;;
    [:hr]
    [:h2 "Step 4: want to re-randomize Ajax/WebSocket connection type?"]
    [:p "Hit your browser's reload/refresh button"]
    [:script {:src "main.js"}] ; Include our cljs target
    ))

(defn wrap-debug [handler]
  (fn [req]
    (timbre/debug "request:" req)
    (handler req)))

(defn redirect-login [req not-logged-in logged-in]
  (if (-> req :session :uid)
    (response/redirect logged-in)
    (response/redirect not-logged-in)))

(defn login-page [req]
  (hiccup/html
    [:h1 "Login"]
    (form/form-to [:post "/login"]
                  (form/text-field "username")
                  (form/drop-down "pronouns" ["they" "she" "he" "it"] "they")
                  (form/submit-button "login"))))

(defn user-login [req]
  (-> (response/redirect "/game")
      (assoc-in [:session :uid] (-> req :params :username))))

(defn user-logout [req]
  (-> (response/redirect "/login")
      (assoc-in [:session :uid] nil)))

(defn require-login [handler]
  (fn [req]
    (if (-> req :session :uid)
      (handler req)
      (response/redirect "/login"))))

(defroutes logged-in-routes
  (GET  "/logout"   req (user-logout req))
  (GET  "/game"     req "game list goes here")
  (GET  "/game/:id" [id] (str "game info for " id))
  (GET  "/chsk"  ring-req (ring-ajax-get-or-ws-handshake ring-req))
  (POST "/chsk"  ring-req (ring-ajax-post                ring-req))
  )

(defroutes logged-out-routes
  (GET  "/login"    req (login-page req))
  (POST "/login"    req (user-login req))
  )

(defroutes ring-routes
  (GET  "/"         req (redirect-login req "/login" "/game"))
  (wrap-routes logged-in-routes require-login)
  logged-out-routes ; todo: accessible only to logged-out users; logged-in users should be redirected to /game
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>"))

; all non-/login paths should redirect to /login if not logged in.
; GET /login serves the login form
; POST /login logs in and redirects to /game
; routes
; / -> /login if not logged in, /game otherwise
; /game -> list of games currently playing
; /game/new -> create new game screen
; /game/join -> list of games to join
; /game/:id -> play interface

(def main-ring-handler
  "**NB**: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
  middleware to work. These are included with
  `ring.middleware.defaults/wrap-defaults` - but you'll need to ensure
  that they're included yourself if you're not using `wrap-defaults`."
  (-> ring-routes
      wrap-debug
      (ring.middleware.defaults/wrap-defaults (assoc ring.middleware.defaults/site-defaults :security nil))))

;;;; Sente event handlers

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

;; TODO Add your (defmethod -event-msg-handler <event-id> [ev-msg] <body>)s here...

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
    (sente/start-server-chsk-router!
      ch-chsk event-msg-handler)))

;;;; Init stuff

; When run standalone, run via httpkit
(defn -main []
  (infof "Starting up...")
  (start-router!)
  (let [stop-fn (http-kit/run-server main-ring-handler)  ;{:port port}
        port (:local-port (meta stop-fn))
        uri (format "http://localhost:%s/" port)]
    (infof "Web server is running at `%s`" uri)
    (try
      (.browse (java.awt.Desktop/getDesktop) (java.net.URI. uri))
      (catch java.awt.HeadlessException _))))
