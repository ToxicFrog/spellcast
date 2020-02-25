(ns spellcast.routes
  (:require [compojure.core :refer [defroutes routes GET POST]]
            [compojure.coercions :refer [as-int]]
            [spellcast.views.join :as join]
            [hiccup.middleware :as hiccup]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [spellcast.text :as text]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.util.response :as r]))

(defn session-to-player [session]
  {:name (session :name)
   :pronouns (keyword (session :pronouns))})

(defn init []
  (println "spellcast is starting"))

(defn destroy []
  (println "spellcast is shutting down"))

(defroutes app-routes
  (GET "/" request
       (let [session (update-in (request :session) [:n] #(if %1 (inc %1) 0))]
         (-> (str
               (text/msg "<%= (:name p1) %> strides confidently into the arena. The referee casts the formal Dispel Magic and Anti-Magic on <%= (prn p1 :obj) %>..."
                 :p1 (session-to-player session))
               "\n\n"
               request)
             (r/response)
             (r/content-type "text/plain")
             (assoc :session session))))
  (GET "/join" request (join/get request))
  (POST "/join" request (join/post request))
  (GET "/part" request
       {:status 302 :headers {"Location" "/"} :body "" :session nil})
  (route/resources "/")
  (route/not-found "Not Found"))

(defn wrap-session-redirect [handler]
  (fn [request]
    (if (and (-> request :session :name nil?) (not= "/join" (request :uri)))
      {:status 302 :headers {"Location" "/join"} :body ""}
      (handler request))))

(defn wrap-session-debug [handler]
  (fn [request]
    (let [response (handler request)]
      (println (request :uri))
      (println ">>" (request :session))
      (println "<<" (response :session))
      (println "")
      response)))

(def app
  (-> (routes app-routes)
      (wrap-session-redirect)
      ; (wrap-session-debug)
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:session :cookie-name] (str "spellcast-" (System/currentTimeMillis)))
            (assoc-in [:security :anti-forgery] false)))
      (hiccup/wrap-base-url)))

(def handler #'app)
