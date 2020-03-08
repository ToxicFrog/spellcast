(ns spellcast.state.player
  (:require [schema.core :as s :refer [def defn defschema fn]]))

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
   :left (s/constrained [Gesture] #(= 8 (count %)))
   :right (s/constrained [Gesture] #(= 8 (count %)))
   ; :status s/Any ; status effects -- map from effect to duration?
   })

(defschema PlayerParams
  {:name s/Str :pronouns s/Str})

(defn ->Player :- Player
  "Create a new Player object."
  [params :- PlayerParams, hp :- s/Int]
  {:name (params :name)
   :pronouns (-> params :pronouns keyword)
   :hp hp
   :left (vec (repeat 8 :nothing))
   :right (vec (repeat 8 :nothing))})

