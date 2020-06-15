(require 'clojure.spec.alpha 'expound.alpha 'io.aviso.repl)
(io.aviso.repl/install-pretty-exceptions)
; (set! clojure.spec.alpha/*explain-out* expound.alpha/printer)

(ns spellcast.routes
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
  (:require
    [clojure.string :as string]
    [compojure.core :refer [defroutes routes GET POST]]
    [compojure.route :as route]
    [hiccup.middleware :refer [wrap-base-url]]
    [io.aviso.repl]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [ring.util.request :as rq]
    [ring.util.response :as r]
    [spellcast.phase.all]
    [spellcast.views.game :as views.game]
    [spellcast.views.join :as views.join]
    [spellcast.views.log :as views.log]
    [spellcast.world :as game]
    ))

(defn init []
  (println "spellcast is starting")
  (io.aviso.repl/install-pretty-exceptions)
  (s/set-fn-validation! true)
  (game/reset-game!)
  (game/log! {} :all "Game starts."))

(defn destroy []
  (println "spellcast is shutting down"))

(defn- logged-in [request]
  (get-in request [:session :name]))

; (defn- debuglog [val] (println val) val)

(defroutes app-routes
  ; Redirect logged-in users to /game, logged-out users to /join
  (GET "/" request
       (cond
         (logged-in request) (r/redirect "/game")
         :else (r/redirect "/join")))
  ; Display the join-game page, or handle a join request
  (GET "/join" request (views.join/get request))
  (POST "/join" request (views.join/post request))
  ; Getters for the in-game UI and game data.
  (GET "/game" request (views.game/page request (logged-in request)))
  (GET "/log" request (views.log/page request (logged-in request)))
  ; Event receptor
  (POST "/game/:evt" [evt :as request]
       (game/POST! (logged-in request) request (rq/body-string request)))
  ; Debug handlers
  (GET "/debug/state" request
       (str "<pre>"
         (with-out-str (pprint (request :session)))
         (with-out-str (pprint (game/state)))
         "</pre>"))
  (GET "/debug/reset" request
       (game/reset-game!)
       {:status 200
        ; :headers {"Location" "/"}
        :headers {"Content-type" "text/plain"}
        :body "state reset"
        :session nil
        :cookies (->> (request :cookies)
                      (map (fn [[k _]] [k {:value "" :max-age 0}]))
                      (into {}))})
  (route/resources "/")
  (route/not-found "Not Found"))

(defn wrap-session-debug [next-handler]
  (fn [request]
    (let [response (next-handler request)]
      (println (request :request-method) (request :uri))
      ; (println ">>" (request :session))
      (println "<<" (dissoc response :headers))
      ; (println "==" (str (game/state)))
      ; (println "")
      response)))

(defn wrap-session-redirect [next-handler]
  (fn [request]
    (let [joined (-> request :session :name some?)
          uri (request :uri)]
      (cond
        ; If they aren't logged in, redirect anything in /game to /join
        (string/starts-with? uri "/debug") (next-handler request)
        (and
          (not joined)
          (not= "/join" uri))
        {:status 302 :headers {"Location" "/join"} :body ""}
        ; If they are logged in, don't let them access /join, bounce them to /game instead
        (and joined (= "/join" uri))
        {:status 302 :headers {"Location" "/game"} :body ""}
        ; Everything else falls through to the rest of the handlers.
        :else (next-handler request)))))

(def app
  (-> (routes app-routes)
      (wrap-session-redirect)
      (wrap-session-debug)
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:session :cookie-name] "spellcast-session") ;(str "spellcast-" (System/currentTimeMillis)))
            (assoc-in [:session :cookie-attrs :max-age] (* 72 60 60))
            (assoc-in [:security :anti-forgery] false)))
      (wrap-base-url)))

(def handler #'app)
