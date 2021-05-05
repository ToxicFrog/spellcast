(ns spellcast.spellbook.basic
  "Basic spellbook for testing. Only single-gesture spells and simple effects."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.spellbook :refer [defspellbook spell-from-ns]]
    ))

(defspellbook basic
  (spell-from-ns 'spellcast.spells.surrender [:palm2])
  (spell-from-ns 'spellcast.spells.counter-spell [:wave :palm :palm])
  (spell-from-ns 'spellcast.spells.counter-spell [:wave :wave :snap])
  (spell-from-ns 'spellcast.spells.stab [:knife])
  (spell-from-ns 'spellcast.spells.shield [:palm])
  (spell-from-ns 'spellcast.spells.missile [:digit])
  ; (spell 'lightning-bolt [:clap2])
  ; (spell-from-ns 'spellcast.spells.stab [:knife])
  ; (spell 'paralysis [:fingers])
  )
