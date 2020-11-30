(ns spellcast.routes-test
  (:require
    [clojure.test :refer :all]
    [clojure.pprint :refer [pprint]]
    [compojure.core :refer [routes]]
    [ring.mock.request :as mock]
    [clojure.data.json :as json]
    [spellcast.core :refer [init]]
    [spellcast.routes :refer [handler' app-routes wrap-session-redirect wrap-session-debug]]
    ))

(defn- get [session path & rest]
  (as-> (mock/request :get path) $
        (apply assoc $ :session session rest)
        (handler' $)
        (do (-> $ :body json/read-str pprint) $)
        ($ :session session)))

(defn- post [session path body & rest]
  (as-> (mock/request :post path) $
        (mock/json-body $ body)
        (apply assoc $ :session session rest)
        (handler' $)
        ($ :session session)))

(deftest integration-test
  (init)
  (let
    [red (post {} "/join" nil :params {:name "Red" :pronouns "she"})
     red (post red "/game/log" {:text "beep beep beep"})
     blu (post {} "/join" nil :params {:name "Blue" :pronouns "they"})
     ; we should now be in game
     blu (post blu "/game/log" {:text "boop boop boop"})
     blu (post blu "/game/gesture" {:gesture :palm :hand :left})
     blu (post blu "/game/gesture" {:gesture :palm :hand :right})
     red (post red "/game/gesture" {:gesture :palm :hand :left})
     red (post red "/game/gesture" {:gesture :knife :hand :right})
     blu (post blu "/game/ready" true)
     red (post red "/game/ready" true)
     ; and now we should be in postgame
     red (get red "/data/log/0")
     ]))
