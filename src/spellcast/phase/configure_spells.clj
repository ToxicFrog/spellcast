(ns spellcast.phase.configure-spells
  "Configure each spell by filling in its :options. If there is only one possible setting for each option no user interaction is required; otherwise this phase sends questions to the casters."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.game :as game]
    [spellcast.data.player :as player]
    [spellcast.data.spell]
    [spellcast.data.spellbook]
    [spellcast.spellbook.basic :refer [basic]]
    [spellcast.logging :refer [log]]
    [spellcast.phase.common :as phase-common :refer [defphase]]
    ))

(defn reify-option [world spell [key option]]
  [key (first ((:domain option) world spell))])

(defn configure-spell
  [world spell]
  (let [options (:options spell)
        config (->> options
                    (map (partial reify-option world spell))
                    (into {}))]
    (merge spell config)))

(defphase configure-spells
  (reply BEGIN [world]
    ; Traverse all spells in the cast buffer. For any ambiguous options, generate
    ; and transmit questions to players.
    world)

  (reply END [world]
    ; Questions have all been answered. Merge spell configuration into spells.
    ; But we haven't implemented questions yet, so instead we just take the first
    ; answer from each option...
    (update world :casting (partial map (partial configure-spell world))))

  (reply INFO [world]
    {:when-ready "Waiting for opponents..."
     :when-unready "Submit Answers"})

  (reply NEXT [world]
    ; In the full version, this will proceed iff all disambiguation questions have
    ; been answered. In this version, there are no disambiguation questions so we
    ; can proceed immediately.
    (when (game/all-players? world :ready)
      :execute-spells))

  ; event handlers for Q&A go here

  (event ready [world player ready?]
    (game/pset world player :ready ready?)))
