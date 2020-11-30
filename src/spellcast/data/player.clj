(ns spellcast.data.player
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    ; we don't need any other requires, but we need the (:require$ for the
    ; boilerplate inserter
    [schema.core]
    ))

(defschema Pronouns
  (s/enum :they :she :he :it))

(defschema Hand
  (s/enum :left :right))

(defschema Gesture
  (s/enum
    :nothing :knife :palm :fingers
    :snap :digit :wave :clap
    :antispell :unseen))

(defschema VisFilter
  "A visibility filter for gestures and other data elements."
  (s/cond-pre
    #{s/Str}
    (s/=> s/Bool, (s/maybe s/Str))))

(defschema GestureRecord
  "A record of the player's gestures for a given turn."
  {:left Gesture :right Gesture (s/optional-key :vis) VisFilter})

(defn- random-gesture []
  (let [gestures [:nothing :knife :palm :fingers :snap :digit :wave :clap :antispell :unseen]
        len (count gestures)
        gst (fn [] (get gestures (rand-int len)))]
    {:left (gst) :right (gst) :vis (constantly true)}))

(defschema Player
  {:name s/Str
   :pronouns Pronouns
   :hp s/Int
   ; Whether or not the player is ready to proceed, i.e. whether they are satisfied
   ; with the gestures they've selected and so forth.
   :ready s/Bool
   ; History of gestures, most recent gesture first.
   :gestures [GestureRecord]
   ; map from effect type to duration
   ; decrements at end of turn, so 1 means the effect will expire at the end of
   ; the current turn.
   ; effects like disease and poison will count down to the point where you die
   ; permanent effects will have value Double/POSITIVE_INFINITY
   :effects {s/Keyword s/Num}
   ; :minions
   ; :minions {s/Keyword }
   ; map from name to (hp, type) pair?
   })

(defschema PlayerParams
  {:name s/Str :pronouns s/Keyword :hp s/Int})

(defn ->Player :- Player
  "Create a new Player object."
  [params :- PlayerParams]
  (assoc params
    :ready false
    ; for testing -- initialize with random gestures
    ;:gestures []
    :gestures (take 4 (repeatedly random-gesture))
    :effects {:protection 3 :haste 1}
    ))

(defn- filter-gesture
  [name record]
  (if ((:vis record) name)
    (dissoc record :vis)
    {:left :unseen :right :unseen}))

(defn with-filtered-gestures :- Player
  "Given a player and the name of another player viewing them, return a view of that player with gesture information filtered according to its visibility rules."
  [player viewer]
  (update player :gestures (partial map (partial filter-gesture viewer))))

(defn new-gestures :- Player
  [{:keys [name] :as player} :- Player]
  (update player :gestures
    #(cons {:left :nothing :right :nothing :vis #{name}} %)))
