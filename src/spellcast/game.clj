(ns spellcast.game
  (:require
    [spellcast.state.player :refer [->Player PlayerParams]]
    [spellcast.state.game :as game :refer [Game ->Game LogFilter]]
    [schema.core :as s :refer [def defn defschema fn]]
    [spellcast.logging :as logging]))

(def SETTINGS {:max-players 2 :max-hp 15})

(def ^:private world
  (atom (->Game SETTINGS) :validator (s/validator Game)))

(defn state [] @world)
(defn reset! [] (swap! world (constantly (->Game SETTINGS))))

(defn add-player! [params :- PlayerParams]
  (swap! world game/add-player (->Player params (-> @world :settings :max-hp))))

(defn get-log [name :- s/Str] :- [s/Str]
  (game/get-log @world name))

(defn log! [bindings :- {s/Keyword s/Any}, & rest] :- Game
  (apply swap! world logging/log bindings rest))
