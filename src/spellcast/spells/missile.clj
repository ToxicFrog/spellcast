(ns spellcast.spells.missile
  "This spell creates a material object of hard substance which is hurled towards the subject of the spell and causes 1 point of damage. The spell is thwarted by a 'shield', in addition to the usual 'counter-spell', 'dispel magic' and 'magic mirror' (the latter causing it to hit whoever cast it instead)."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.domain :refer [living enemy]]
    [spellcast.data.game :as game]
    [spellcast.logging :refer [log]]
    [spellcast.data.spell :refer :all]
    ))

(spell "Missile"
  :type :spell)

(option :target
  "Who do you want to cast Missile on?"
  :domain living
  :prefer enemy)

; Interact with other missile: stack them together.
(interact :missile same-target?
  [self other]
  [(update self :n #(inc (or % 1)))])

(defn- log-opts [{:keys [n] :as self}]
  (if (> (or n 1) 1)
    (assoc self
      :missile "Missiles"
      :shatters "shatter"
      :strikes "strike"
      :dispelled "are dispelled"
      :countered "are destroyed")
    (assoc self
      :missile "Missile"
      :shatters "shatters"
      :strikes "strikes"
      :dispelled "is dispelled"
      :countered "is destroyed")))

(defn finalize [world {:keys [target] :as self}]
  (cond
    (:countered self)
    (-> world
        (log (log-opts self)
          target "The {{missile}} targeting you {{dispelled}}."
          :else "The {{missile}} targeting {{target}} {{dispelled}}."))
    :else
    (-> world
        (log (log-opts self)
          target "The {{missile}} {{strikes}} you."
          :else "The {{missile}} {{strikes}} {{target}}.")
        (game/pupdate target :hp dec))))
