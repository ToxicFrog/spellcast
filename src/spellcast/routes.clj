(require '[io.aviso.repl])
(io.aviso.repl/install-pretty-exceptions)

(ns spellcast.routes
  (:require [compojure.core :refer [defroutes routes GET POST]]
            [compojure.coercions :refer [as-int]]
            [schema.core :as s]
            [spellcast.views.join :as join]
            [spellcast.views.game :as vgame]
            [hiccup.middleware :as hiccup]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [spellcast.text :as text]
            [spellcast.game :as game]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.util.response :as r]))

(defn init []
  (println "spellcast is starting")
  (s/set-fn-validation! true)
  (game/reset!))

(defn destroy []
  (println "spellcast is shutting down"))

(defn- logged-in [request]
  (some? (get-in request [:session :name])))

(defroutes app-routes
  (GET "/" request
       (cond
         (logged-in request) (r/redirect "/game")
         :else (r/redirect "/join")))
  (GET "/join" request
       (if (logged-in request)
         (r/redirect "/game")
         (join/get request)))
  (POST "/join" request
       (if (logged-in request)
         (r/redirect "/game")
         (join/post request)))
  (GET "/game" request
       (if (logged-in request)
         (str (game/state))
         (r/redirect "/join")))
  (GET "/part" request
       (game/reset!)
       {:status 302
        :headers {"Location" "/"}
        :body ""
        :session nil
        :cookies (->> (request :cookies)
                      (map (fn [[k v]] [k {:value "" :max-age 0}]))
                      (into {}))})
  (route/resources "/")
  (route/not-found "Not Found"))

(defn wrap-session-debug [handler]
  (fn [request]
    (let [response (handler request)]
      (println (request :uri))
      (println ">>" (request :session))
      (println "<<" (response :session))
      (println "==" (str (game/state)))
      (println "")
      response)))

(def app
  (-> (routes app-routes)
      ; (wrap-session-debug)
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:session :cookie-name] "spellcast-session") ;(str "spellcast-" (System/currentTimeMillis)))
            (assoc-in [:session :cookie-attrs :max-age] (* 72 60 60))
            (assoc-in [:security :anti-forgery] false)))
      (hiccup/wrap-base-url)))

(def handler #'app)
