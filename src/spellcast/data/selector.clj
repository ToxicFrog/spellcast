(ns spellcast.data.selector
  "Functions for selecting targets from the world, i.e. the domain of a spell's function."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [schema.core]
    [spellcast.data.game :refer [Game]]
    [spellcast.data.spell :refer [Spell]]
    ))

(defschema Target
  (s/cond-pre s/Keyword s/Str))

; TODO: investigate how to handle stuff like blindness and invisibility -- a
; blind wizard can't target anything except themself, and an invisible wizard
; can't be targeted by other wizards or hostiles. So probably (living) should
; check the caster and exclude all invisible wizards who aren't the caster, and
; if the caster is blind, include only the caster.

(defn wizards :- #{Target}
  "A selector that returns all living beings in the world. At the moment this means all wizards with hp>0. TODO: include minions, elementals, and whatnot."
  [world :- Game, spell :- Spell]
  (as-> world $
       (:players $)
       (filter (fn [[k v]] (> (:hp v) 0)) $)
       (map first $)
       (set $)
       (conj $ :nothing)))

; TODO once monsters and stuff exist, expand this to cover them
(def living wizards)

; We should probably make including :nothing the default, since most spells are nothing-targetable. Exceptions below.
; Wizard only: Dispel Magic, Summon Elemental
; Wizard or nothing: Delayed Effect, Summon Monster, Antispell, Permanency
; No targeting: Surrender, Firestorm, Icestorm
; Special: Raise Dead

(defn requires-target :- #{Target}
  "A selector that removes :nothing from the set of targets returned by the selector it wraps. Used for spells that require a valid target, which, in the Bartle spellbook, is Dispel Magic, Delayed Effect, Antispell, Permanency, all Summons, and Raise Dead."
  [f]
  (fn [world :- Game, spell :- Spell]
    (disj (f world spell) :nothing)))

(defn self :- #{Target}
  "A selector that returns the caster of the spell."
  [world :- Game, spell :- Spell]
  #{(:caster spell)})

(defn enemy :- #{Target}
  "A selector that returns all enemies of the caster. At the moment this is everyone except the caster. TODO: exclude the caster's minions, too."
  [world :- Game, spell :- Spell]
  (-> (living spell world)
      set
      (disj (:caster spell))))
