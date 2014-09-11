(ns spellcast.game.collect-gestures)
(require '[clojure.core.async :as async :refer [<! >! <!! >!! chan go pub close! thread]]
         '[spellcast.util :refer :all]
         '[spellcast.game.common :refer :all]
         '[taoensso.timbre :as log])

(defn- pop-conj [xs x]
  (conj (pop xs) x))

(defn- set-gestures [player left right]
  (assoc player :gestures [left right]))

(defn- commit-gestures [player]
  (let [[left right] (:gestures player)]
    (merge-with conj player {:left left :right right})))

(defphase collect-gestures phase-defaults
  (defn done? [game]
    (every-player? game :ready))
  (defn begin [game]
    (log/info "Collecting gestures for turn" (:turn game))
    (-> game
        unready-all
        (update-players assoc :gestures [nil nil])))
  (defn end [game]
    (log/info "Got gestures for all players.")
    (-> game
        (update-players commit-gestures)))
  (defn :gestures [game id left right]
    (update-in game [:players id]
               set-gestures left right)))
