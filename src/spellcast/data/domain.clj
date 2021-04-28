(ns spellcast.data.domain
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

(defn living [world spell]
  (->> world
       :players
       (filter (fn [[k v]] (> (:hp v) 0)))
       (map first)))

(defn also [f &rest]
  (fn [world spell]
    (concat (f world spell) rest)))

(defn self [world spell]
  [(:caster spell)])

(defn enemy [spell world]
  (-> (living spell world)
      set
      (disj (:caster spell))))
