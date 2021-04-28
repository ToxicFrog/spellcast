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

