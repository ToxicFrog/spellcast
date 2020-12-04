(ns spellcast.data.spellbook
  "Types and functions for spellbooks and spells."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.spell :refer [SpellPage]]
    ))

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
  [name gestures options invoke resolve]
  ; We wrap it in [] because it's going to be mapcatted by defspellbook and
  ; maps are, themselves, seqable.
  [{:name name
   :gestures gestures
   :options options
   :invoke invoke
   :resolve resolve}])

(defn option
  [question domain default]
  {:question question :domain domain :default default})
