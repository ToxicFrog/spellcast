(ns spellcast.routes
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [clojure.string :as string]
    [compojure.coercions :refer [as-int]]
    [compojure.core :refer [defroutes routes GET POST]]
    [compojure.route :as route]
    [hiccup.middleware :refer [wrap-base-url]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [ring.middleware.json :refer [wrap-json-body wrap-json-params wrap-json-response]]
    [ring.util.request :as rq]
    [ring.util.response :as r]
    [spellcast.phase.all]
    [spellcast.views.game :as views.game]
    [spellcast.views.join :as views.join]
    [spellcast.views.log :as views.log]
    [spellcast.world :as world]
    ))

(defn- logged-in [request]
  (get-in request [:session :name]))

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
  (GET "/log/:index" [index :<< as-int :as request]
       (views.log/page (logged-in request) index))
  ; Event receptor
  (POST "/game/:evt" [evt :as request]
       (world/POST! (logged-in request) request (request :body)))
  ; Debug handlers
  (GET "/debug/state" request
       (str "<pre>"
         (with-out-str (pprint (request :session)))
         (with-out-str (pprint (world/state)))
         "</pre>"))
  (GET "/debug/reset" request
       (world/reset-game!)
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
  (fn [{:keys [request-method uri session headers params body] :as request}]
    (debug ">>" request-method uri session
      "\n -headers" headers
      "\n -params" params
      "\n -body" body)
    (let [response (next-handler request)]
      (debug "<<" response) ;(dissoc response :headers))
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

(def handler'
  "The inner parts of the handler, for testing."
  (-> (routes app-routes)
      (wrap-session-redirect)
      (wrap-session-debug)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      ))

(def handler
  (-> handler'
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:session :cookie-name] "spellcast-session") ;(str "spellcast-" (System/currentTimeMillis)))
            (assoc-in [:session :cookie-attrs :max-age] (* 72 60 60))
            (assoc-in [:security :anti-forgery] false)))
      (wrap-base-url)))
