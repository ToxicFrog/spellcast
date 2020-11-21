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

(defschema Gesture
  (s/enum
    :nothing :knife :palm :fingers
    :snap :digit :wave :clap
    :antispell :unseen))

(defschema VisFilter
  "A visibility filter for gestures and other data elements."
  (s/cond-pre
    #{s/Str}
    (s/=> (s/maybe s/Str) s/Bool)))

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
   ; TODO right now this just displays unconditionally, but in practice each turn
   ; of gestures needs to be annotated with who it's visible to; gestures currently
   ; being entered are visible only to the owner until the collect-gestures phase
   ; is over, gestures made while invisible are only ever visible to the owner,
   ; gestures are never visible to someone who was blind when they were being made, etc
   :gestures [GestureRecord]
   ; :status s/Any ; status effects -- map from effect to duration?
   ; :creatures ...
   })

(defschema PlayerParams
  {:name s/Str :pronouns s/Str
   s/Keyword s/Str})

(defn ->Player :- Player
  "Create a new Player object."
  [params :- PlayerParams, hp :- s/Int]
  {:name (params :name)
   :pronouns (-> params :pronouns keyword)
   :hp hp
   ; for testing -- initialize with random gestures
   ;:gestures []
   :gestures (take 4 (repeatedly random-gesture))
   })

(defn- filter-gesture
  [name record]
  (if ((:vis record) name)
    (dissoc record :vis)
    {:left :unseen :right :unseen}))

(defn with-filtered-gestures :- Player
  "Given a player and the name of another player viewing them, return a view of that player with gesture information filtered according to its visibility rules."
  [player viewer]
  (update player :gestures (partial map (partial filter-gesture viewer))))
