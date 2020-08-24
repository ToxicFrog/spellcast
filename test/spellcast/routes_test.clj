(ns spellcast.routes-test
  (:require
    [clojure.test :refer :all]
    [compojure.core :refer [routes]]
    [ring.mock.request :as mock]
    [spellcast.core :refer [init]]
    [spellcast.routes :refer [handler' app-routes wrap-session-redirect wrap-session-debug]]
    ))

(defn- get [session path & rest]
  (as-> (mock/request :get path) $
        (apply assoc $ :session session rest)
        (handler' $)
        ($ :session session)))

(defn- post [session path body & rest]
  (as-> (mock/request :post path) $
        (mock/json-body $ body)
        (apply assoc $ :session session rest)
        (handler' $)
        ($ :session session)))

(deftest integration-test
  (init)
  (-> {}
    (post "/join" nil :params {:name "Red" :pronouns "she"})
    (post "/game/log" {:text "beep beep beep"})
    (get  "/log/0"))
  (-> {}
    (post "/join" nil :params {:name "Blue" :pronouns "they"})
    (post "/game/log" {:text "boop boop boop"})
    (get  "/log/0"))
  )
