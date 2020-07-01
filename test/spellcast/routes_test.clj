(ns spellcast.routes-test
  (:require
    [clojure.test :refer :all]
    [compojure.core :refer [routes]]
    [ring.mock.request :as mock]
    [spellcast.routes :refer [init app-routes wrap-session-redirect wrap-session-debug]]
    ))

(def handler
  (-> (routes app-routes)
      (wrap-session-redirect)
      (wrap-session-debug)))

(defn- get [session path & rest]
  (as-> (mock/request :get path) $
        (apply assoc $ :session session rest)
        (handler $)
        ($ :session session)))

(defn- post [session path & rest]
  (as-> (mock/request :post path) $
        (apply assoc $ :session session rest)
        (handler $)
        ($ :session session)))

(deftest integration-test
  (init)
  (-> {}
    (post "/join" :params {:name "Red" :pronouns "she"})
    (post "/game/log" :body "beep beep beep")
    (get  "/log/0"))
  (-> {}
    (post "/join" :params {:name "Blue" :pronouns "they"})
    (post "/game/log" :body "boop boop boop")
    (get  "/log/0"))
  )
