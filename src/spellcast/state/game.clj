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

(defn ->Game :- Game
  "Create a new game with the given configuration settings."
  [settings :- GameSettings]
  {:players {}
   :settings settings
   :log []})

(defn add-log :- Game
  "Add a message to the game log. It will be visible only to players for whom (filter playername) returns true."
  [world :- Game, message :- s/Str, filter :- LogFilter]
  (update world :log conj [message filter]))

(defn get-log :- [s/Str]
  "Return the log messages visible to the given player. Note that when requested by someone who is not logged in, name may be nil."
  [world :- Game, name :- s/Str]
  (->> (world :log)
       (filter (fn [[_ vis]] (vis name)))
       (map first)))

(defn add-player :- Game
  "Add a new player to the game. Throws if someone with that name is already present."
  [world :- Game, player :- Player]
  (if (contains? (world :players) (player :name))
    (throw (IllegalArgumentException. (str "A player named " (player :name) " is already in the arena.")))
    (assoc-in world [:players (player :name)] player)))

(defn get-player :- Player
  "Return a player with the given name, or nil."
  [world :- Game, name :- s/Str]
  (get-in world [:players name]))
