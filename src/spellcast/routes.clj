(require '[io.aviso.repl])
(io.aviso.repl/install-pretty-exceptions)

(ns spellcast.routes
  (:require [compojure.core :refer [defroutes routes GET POST]]
            [compojure.coercions :refer [as-int]]
            [schema.core :as s]
            [spellcast.views.join :as views.join]
            [spellcast.views.game :as views.game]
            [hiccup.middleware :refer [wrap-base-url]]
            [hiccup.util :refer [escape-html]]
            [ring.util.request :as rq]
            [clojure.pprint]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [spellcast.game :as game]
            [clojure.string :as string]
            [spellcast.phase.pregame]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.util.response :as r]))

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

(defn- debuglog [val] (println val) val)

(defroutes app-routes
  (GET "/" request
       (cond
         (logged-in request) (r/redirect "/game")
         :else (r/redirect "/game/join")))
  (GET "/game" request (views.game/page request))
  (GET "/debug/state" request
       (str "<pre>"
         (with-out-str (clojure.pprint/pprint (request :session)))
         (with-out-str (clojure.pprint/pprint (game/state)))
         "</pre>"))
  (GET "/debug/reset" request
       (game/reset-game!)
       {:status 302
        :headers {"Location" "/"}
        :body ""
        :session nil
        :cookies (->> (request :cookies)
                      (map (fn [[k v]] [k {:value "" :max-age 0}]))
                      (into {}))})
  (GET "/game/:evt" [evt :as request]
       (game/dispatch-event! (logged-in request) request))
  (POST "/game/:evt" [evt :as request]
       (game/dispatch-event! (logged-in request) request (rq/body-string request)))
  (route/resources "/")
  (route/not-found "Not Found"))

(defn wrap-session-debug [handler]
  (fn [request]
    (let [response (handler request)]
      (println (request :request-method) (request :uri))
      (println ">>" (request :session))
      (println "<<" (response :session))
      (println "==" (str (game/state)))
      (println "")
      response)))

(defn wrap-session-redirect [handler]
  (fn [request]
    (let [joined (-> request :session :name some?)
          uri (request :uri)]
      (println "wrap-redirect" joined uri)
      (cond
        ; If they aren't logged in, redirect anything in /game to /join/game
        (and (not joined)
          (string/starts-with? uri "/game")
          (not= "/game/join" uri))
        {:status 302 :headers {"Location" "/game/join"} :body ""}
        ; If they are logged in, don't let them access /game/join, bounce them to /game instead
        (and joined (= "/game/join" uri))
        {:status 302 :headers {"Location" "/game"} :body ""}
        ; Everything else falls through to the rest of the handlers.
        :else (handler request)))))

(def app
  (-> (routes app-routes)
      (wrap-session-debug)
      (wrap-session-redirect)
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:session :cookie-name] "spellcast-session") ;(str "spellcast-" (System/currentTimeMillis)))
            (assoc-in [:session :cookie-attrs :max-age] (* 72 60 60))
            (assoc-in [:security :anti-forgery] false)))
      (wrap-base-url)))

(def handler #'app)
