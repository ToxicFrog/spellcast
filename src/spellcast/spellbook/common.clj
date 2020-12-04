(ns spellcast.spellbook.common
  "Common utilities for spells and spellbooks, like target selection and logging cast messages."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.game :as game :refer [Game]]
    [spellcast.data.spell :refer [Spell]]
    [spellcast.data.spellbook]
    [spellcast.logging :refer [log]]
    ))

(defn- handedness [hand]
  (condp = hand
    :left "the left hand"
    :right "the right hand"
    :both "both hands"))

(defn at-target [target]
  (condp = target
    :everything "over the arena"
    :nothing "up into the air"
    (str "at " target)))

(defn player? [target]
  ; HACK HACK HACK
  ; right now a string is always a player and a keyword is always into air/over arena
  ; this may change once monsters are introduced, etc
  (string? target))

(defn default-invoke
  "Default invokation function for spells. Logs a message of the form '<player> casts <spell> (with the <hand>) at <target>'."
  [world :- Game, {:keys [caster hand target] :as spell} :- Spell]
  (let [caster-obj (game/get-player world caster)
        params (assoc spell
                 :hand (handedness hand)
                 :at-target (at-target target)
                 :caster-obj caster-obj)]
    (cond
      (= caster target)
      (log world params
        caster "You cast {{name}} (with {{hand}}) at yourself."
        :else "{{caster}} casts {{name}} (with {{hand}}) at {{pronoun caster-obj :ref}}.")
      ; HACK HACK HACK -- one wizard casting at another
      ; in future this needs to properly handle casting at monsters, etc.
      (player? target)
      (log world params
        caster "You cast {{name}} (with {{hand}}) {{at-target}}."
        target "{{caster}} casts {{name}} (with {{hand}}) at you."
        :else "{{caster}} casts {{name}} (with {{hand}} {{at-target}}.")
      ; casting into the air/over the arena
      ; only difference is that we don't lot a message for 'target' since it's not a player
      (keyword? target)
      (log world params
        caster "You cast {{name}} (with {{hand}}) {{at-target}}."
        :else "{{caster}} casts {{name}} (with {{hand}} {{at-target}}.")
      :else
      (log world params
        :all "Something went horribly wrong.")
      )))
