(ns spellcast.spells.shield
  "flavour text goes here"
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.selector :as domain]
    [spellcast.data.spell :refer :all]
    [spellcast.data.game :as game]
    ; [spellcast.data.spellbook :refer [defspellbook spell option]]
    [spellcast.logging :refer [log]]
    ; [spellcast.spellbook.common :refer [default-invoke]]
    ))

(spell "Shield"
  :type :spell
  :priority 10)

(option :target "Who do you want to cast Shield on?"
  :domain (domain/also domain/living :nothing)
  :prefer domain/self)

; Interacts with any other spell, but not with non-spells like stab, surrender, etc.
(interact #{:knife :missile} same-target?
  [self other]
  [self (assoc other :shielded true)])

; TODO: countered and dispelled are both very common effects; we should have default
; messages for them, and default handles for them that are invoked instead of
; :finalize as needed.
; Perhaps spells need a :result field which can be :countered, :dispelled, etc
; and if set one of those is called instead of :finalize
; this implies that our result-setting API sets it iff it's not already set
(defn countered [world self]
  (log world self
    (:target self) "Your Shield is countered."
    :else "The shield on {{target}} is countered."))

(defn dispelled [world self]
  (log world self
    (:target self) "Your Shield is dispelled."
    :else "The shield on {{target}} is dispelled."))

; Finalize does nothing because all the important parts happened during spell
; interactions.
(defn finalize :- game/Game
  [world :- game/Game, self :- Spell]
  (cond
    (:dispelled self) (dispelled world self)
    (:countered self) (countered world self)
    :else world))
