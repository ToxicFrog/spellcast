(ns spellcast.game
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
  (:require
    [spellcast.logging :as logging]
    [spellcast.state.event :as event]
    [spellcast.state.game :as game :refer [Game ->Game]]
    ))

(def SETTINGS {:max-players 2 :max-hp 15})

(def ^:private world
  (atom (->Game SETTINGS) :validator (s/validator Game)))

(defn state [] @world)
(defn reset-game! [] (reset! world (->Game SETTINGS)))

(defn get-log :- [s/Str]
  [name :- s/Str]
  (game/get-log @world name))

(defn log! :- Game
  "Log one or messages. Same arguments as logging/log.
  Non-transactional (other things may happen in between log messages) but messages are guaranteed to be logged in the order they appear here. Log messages from multiple simultaneous calls to log! may be interleaved, but all players are guaranteed to see the messages in the same order."
  [bindings :- {s/Keyword s/Any}, & rest]
  (apply swap! world logging/log bindings rest))

(defn dispatch-event! :- (s/cond-pre s/Str {s/Keyword s/Any})
  [& rest]
  (let [response (atom nil)]
    (swap! world
      (fn event-swapper [world]
        (let [[world' response'] (apply event/dispatch world rest)]
          (reset! response response')
          world')))
    @response))
