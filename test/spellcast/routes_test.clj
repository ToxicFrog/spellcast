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

(defn gestures [session left right]
  (-> session
      (post "/game/gesture" {:gesture left :hand :left})
      (post "/game/gesture" {:gesture right :hand :right})
      (post "/game/ready" true)))

(deftest integration-test
  (init)
  (let
    [red (post {} "/join" nil :params {:name "Red" :pronouns "she"})
     red (post red "/game/log" {:text "beep beep beep"})
     blu (post {} "/join" nil :params {:name "Blue" :pronouns "they"})
     ; we should now be in game
     blu (post blu "/game/log" {:text "boop boop boop"})
     ; blue casts shield, red casts stab and missile
     blu (gestures blu :palm :snap)
     red (gestures red :knife :digit)
     ; blue surrenders
     blu (gestures blu :palm :palm)
     red (gestures red :clap :clap)
     ; and now we should be in postgame
     red (get red "/data/log/0")
     ]
    true))
