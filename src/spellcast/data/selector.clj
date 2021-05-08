(ns spellcast.data.selector
  "Functions for selecting targets from the world, i.e. the domain of a spell's function."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    ; we don't need any other requires, but we need the (:require) for the
    ; boilerplate inserter
    [schema.core]
    ))

; TODO: investigate how to handle stuff like blindness and invisibility -- a
; blind wizard can't target anything except themself, and an invisible wizard
; can't be targeted by other wizards or hostiles. So probably (living) should
; check the caster and exclude all invisible wizards who aren't the caster, and
; if the caster is blind, include only the caster.

(defn living
  "A selector that returns all living beings in the world. At the moment this means all wizards with hp>0. TODO: include minions, elementals, and whatnot."
  [world spell]
  (->> world
       :players
       (filter (fn [[k v]] (> (:hp v) 0)))
       (map first)))

; We should probably make including :nothing the default, since most spells are nothing-targetable. Exceptions below.
; Wizard only: Dispel Magic, Summon Elemental
; Wizard or nothing: Delayed Effect, Summon Monster, Antispell, Permanency
; No targeting: Surrender, Firestorm, Icestorm
; Special: Raise Dead

(defn also
  "A selector that adds &rest to whatever targets f returns, so you can e.g. do (also living :nothing) for a spell that has 'up into the air' as a valid target."
  [f & rest]
  (fn [world spell]
    (concat (f world spell) rest)))

(defn self
  "A selector that returns the caster of the spell."
  [world spell]
  [(:caster spell)])

(defn enemy
  "A selector that returns all enemies of the caster. At the moment this is everyone except the caster. TODO: exclude the caster's minions, too, and prioritize the most threatening wizard."
  [spell world]
  (-> (living spell world)
      set
      (disj (:caster spell))))
