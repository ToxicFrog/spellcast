(ns spellcast.data.spellbook
  "Types and functions for spellbooks and spells."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.spell :refer [SpellPage Spell]]
    [spellcast.data.game :as game]
    [spellcast.logging :refer [log]]
    ))

(defschema Game
  (s/recursive #'spellcast.data.game/Game))

(defschema Spellbook
  [SpellPage])

(defn expand-spellbooks [sb]
  (println "expand?" sb)
  (if (map? sb) [sb] sb))

(defn prioritize-spell [idx spell]
  (println "prioritize spell" idx spell)
  (assoc spell :priority idx))

(defmacro defspellbook
  "Define a spellbook. Should be passed any number of spells and/or other spellbooks -- it will contain the given spells, and all the contents of the provided spellbooks, in order."
  [name & spells]
  `(s/def ~name :- [SpellPage]
          (map-indexed prioritize-spell (mapcat expand-spellbooks [~@spells]))))

(defn spell :- [(s/one SpellPage "spell")]
  "Create a spell. Should only be called inside (defspellbook) -- otherwise important parts of the spell will be missing."
  [name gestures options invoke finalize]
  ; We wrap it in [] because it's going to be mapcatted by defspellbook and
  ; maps are, themselves, seqable.
  [{:name name
   :gestures gestures
   :options options
   :invoke invoke
   :finalize finalize}])

(defn- ns-get
  ([ns key] (ns-get ns key nil))
  ([ns key default]
   (or
     (some-> ns (ns-resolve key) var-get)
     default)))

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

(defn default-invoke :- Game
  "Default invokation function for spells. Logs a message of the form '<player> casts <spell> (with the <hand>) at <target>'."
  [world :- Game, {:keys [caster hand target] :as spell} :- Spell]
  (let [params (assoc spell
                 :hand (handedness hand)
                 :at-target (at-target target))]
    (info "In default-invoke function for" (:name spell))
    (cond
      (= caster target)
      (log world params
        caster "You cast {{name}} (with {{hand}}) at yourself."
        :else "{{caster}} casts {{name}} (with {{hand}}) at {{themself caster}}.")
      ; HACK HACK HACK -- one wizard casting at another
      ; in future this needs to properly handle casting at monsters, etc.
      (player? target)
      (log world params
        caster "You cast {{name}} (with {{hand}}) {{at-target}}."
        target "{{caster}} casts {{name}} (with {{hand}}) at you."
        :else "{{caster}} casts {{name}} (with {{hand}} {{at-target}}.")
      ; casting into the air/over the arena
      ; only difference is that we don't log a message for 'target' since it's not a player
      (keyword? target)
      (log world params
        caster "You cast {{name}} (with {{hand}}) {{at-target}}."
        :else "{{caster}} casts {{name}} (with {{hand}}) {{at-target}}.")
      :else
      (log world params
        :all "Something went horribly wrong.")
      )))

(defn spell-from-ns :- SpellPage
  [ns-sym gestures]
  (require ns-sym)
  (let [spell-ns (the-ns ns-sym)]
    (assoc (ns-get spell-ns 'properties)
      :gestures gestures
      :options (ns-get spell-ns 'options)
      :interactions (ns-get spell-ns 'interactions)
      :invoke (ns-get spell-ns 'invoke default-invoke)
      :finalize (ns-get spell-ns 'finalize))))
