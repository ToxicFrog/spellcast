(ns spellcast.data.game
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.player :as player :refer [Player VisFilter Gesture Hand]]
    ))

(defschema LogMessage
  "A log message consists of a log line followed by a set of players the log message is meant to be visible to, identified by name.
  A log can also be marked as visible to :all, in which case all players can see it."
  [(s/one s/Str "message") (s/one VisFilter "vis")])

(defschema GameSettings
  {:max-players (s/constrained s/Int (partial < 1))
   :max-hp (s/constrained s/Int pos?)})

(defschema GamePhase
  "Which phase of the game we're in, which in turn determines which events are legal from players and how they are handled."
  (s/enum
    ; waiting for all players to connect; ends by placing all players in the
    ; arena and moving to ingame
    :pregame
    ; test phase that arbitrarily picks a winner and then moves to postgame
    :ingame
    ; permits viewing the log and talking but not entering any actions
    :postgame
    ))

(defschema Game
  {:players {s/Str Player}
   :settings GameSettings
   :phase GamePhase
   :log [LogMessage]})

(defn ->Game :- Game
  "Create a new game with the given configuration settings."
  [settings :- GameSettings]
  {:players {}
   :settings settings
   :phase :pregame
   :log []})

(defn add-log :- Game
  "Add a message to the game log. It will be visible only to players for whom (filter playername) returns true."
  [world :- Game, message :- s/Str, filter :- VisFilter]
  (update world :log conj [message filter]))

(defn get-log :- [s/Str]
  "Return the log messages visible to the given player. Note that when requested by someone who is not logged in, name may be nil."
  [world :- Game, name :- (s/maybe s/Str)]
  (->> (world :log)
       (filter (fn [[_ vis]] (vis name)))
       (map first)))

(defn- map-vals [f kvs]
  (into {}
    (map (fn [[k v]] [k (f v)]) kvs)))

(defn get-filtered-players :- {s/Str Player}
  "Return the player map, with gesture record filters applied."
  [world :- Game, player :- (s/maybe s/Str)]
  (map-vals #(player/with-filtered-gestures % player) (world :players)))

(defn add-player :- Game
  "Add a new player to the game. Throws if someone with that name is already present."
  [world :- Game, player :- Player]
  (if (contains? (world :players) (player :name))
    (throw (IllegalArgumentException. (str "A player named " (player :name) " is already in the arena.")))
    (assoc-in world [:players (player :name)] player)))

(defn get-player :- (s/maybe Player)
  "Return a player with the given name, or nil."
  [world :- Game, name :- s/Str]
  (get-in world [:players name]))

(defn set-gesture :- Game
  [world :- Game, name :- s/Str, hand :- Hand, gesture :- Gesture]
  (update-in world [:players name]
    (fn [player]
      (let [g (-> player :gestures first)
            gs (-> player :gestures rest)
            g' (assoc g hand gesture)]
        (assoc player :gestures (cons g' gs))))))

(defn get-gestures :- [(s/one Gesture "left") (s/one Gesture "right")]
  [world :- Game, name :- s/Str]
  (as-> world $
        (get-player $ name)
        (:gestures $)
        (first $)
        [(:left $) (:right $)]))
