(ns spellcast.state.game
  (:require
    [spellcast.state.player :as player :refer [Player]]
    [schema.core :as s :refer [def defn defschema fn]]))

(defschema LogFilter
  (s/cond-pre
    #{s/Str}
    (s/=> (s/maybe s/Str) s/Bool)))

(defschema LogMessage
  "A log message consists of a log line followed by a set of players the log message is meant to be visible to, identified by name.
  A log can also be marked as visible to :all, in which case all players can see it."
  [(s/one s/Str "message") (s/one LogFilter "vis")])

(defschema GameSettings
  {:max-players (s/constrained s/Int (partial < 1))
   :max-hp (s/constrained s/Int pos?)})

(defschema Game
  {:players {s/Str Player}
   :settings GameSettings
   :log [LogMessage]})

(defn ->Game [settings :- GameSettings] :- Game
  {:players {}
   :settings settings
   :log []})

(defn add-log [world :- Game, message :- s/Str, filter :- LogFilter] :- Game
  (update world :log conj [message filter]))

(defn get-log [world :- Game, name :- s/Str] :- [s/Str]
  (->> (world :log)
       (filter (fn [[_ vis]] (vis name)))
       (map first)))

(defn add-player [world :- Game, player :- Player]
  (if (contains? (world :players) (player :name))
    (throw (IllegalArgumentException. (str "A player named " name " is already in the arena.")))
    (assoc-in world [:players (player :name)] player)))

(defn get-player [world :- Game, name :- s/Str] :- Player
  (get-in world [:players name]))
