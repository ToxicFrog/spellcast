(ns spellcast.spells.counter-spell
  "Any other spell cast upon the subject in the same turn has no effect whatever. In the case of blanket-type spells, which affect more than one person, the subject of the 'counter-spell' alone is protected. For example, a 'fire storm' spell would not affect a wizard if that wizard was simultaneously the subject of a 'counterspell', but everyone else would be affected as usual (unless they had their own protection.)
  The 'counter-spell' will cancel all the spells cast at the subject for that turn, including 'remove enchantment' and 'magic mirror', but not 'dispel magic' or 'finger of death'. It will combine with another spell of its own type for the same effect as if it were alone.
  The 'counter-spell' will also act as a 'shield' on its subject, in addition to its other properties.
  The spell has two alternative gesture sequences, either of which may be used at any time."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.domain :as domain]
    [spellcast.data.spell :refer :all]
    ; [spellcast.data.game :as game]
    ; [spellcast.data.spellbook :refer [defspellbook spell option]]
    [spellcast.logging :refer [log]]
    ; [spellcast.spellbook.common :refer [default-invoke]]
    ))

(spell "Counter-Spell"
  :type :spell)

(option :target "Who do you want to cast Counter-Spell on?"
  :domain (domain/also domain/living :nothing) :prefer domain/self)

; Interacts with any other spell, but not with non-spells like stab, surrender, etc.
(interact :any (all-of spell? same-target?)
  [self other]
  [self (assoc other :countered true)])

(defn dispelled [world self]
  (log world self
    (:target self) "The Counter-Spell on you is subsumed by the Dispel Magic."
    :else "The Counter-Spell on {{target}} is subsumed by the Dispel Magic."))

(defn completed [world self]
  (log world self
    (:target self) "Your magical senses go numb as a Counter-Spell surrounds you."
    :else "A Counter-Spell flares around {{target}}."))

