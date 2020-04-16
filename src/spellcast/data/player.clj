(ns spellcast.data.player
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
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

(defschema Player
  {:name s/Str
   :pronouns Pronouns
   :hp s/Int
   ; TODO right now this just displays unconditionally, but in practice each turn
   ; of gestures needs to be annotated with who it's visible to; gestures currently
   ; being entered are visible only to the owner until the collect-gestures phase
   ; is over, gestures made while invisible are only ever visible to the owner,
   ; gestures are never visible to someone who was blind when they were being made, etc
   :left (s/constrained [Gesture] #(= 8 (count %)))
   :right (s/constrained [Gesture] #(= 8 (count %)))
   ; :status s/Any ; status effects -- map from effect to duration?
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
   :left (vec (repeat 8 :nothing))
   :right (vec (repeat 8 :nothing))})

