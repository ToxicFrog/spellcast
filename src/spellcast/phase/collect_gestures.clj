(ns spellcast.phase.collect-gestures
  "Collects gestures from all active players. At the end of this phase, all players who are capable of entering gestures for this turn will have done so, and their gestures will be recorded in the head of the :gestures list."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.game :as game]
    [spellcast.data.player :as player]
    [spellcast.logging :refer [log]]
    [spellcast.phase.common :as phase-common :refer [defphase]]
    ))

(def user-gestures
  #{:nothing :knife :palm :fingers :wave :clap :snap :digit})

(defn- valid-gesture
  "Perform validation on an attempted gesture set request. The rules are:
  - only 'real' gestures allowed (no :antispell, :unseen, etc)
  - no setting both hands to :knife"
  [world player gesture]
  (let [has-knife? (-> (game/get-gestures world player) set :knife)]
    (cond
      (not (user-gestures gesture)) false
      (and has-knife? (= :knife gesture)) false
      :else true)))

(defphase collect-gestures
  (reply BEGIN [world]
    (-> world
        (update :turn inc)
        (log {:turn (inc (:turn world))}
          :all "--- Turn {{turn}} starts.")
        (game/map-players player/new-gestures)
        (game/map-players #(assoc % :ready false))))
  (reply END [world] world)

  (reply INFO [world]
    {:when-ready "Waiting for opponents..."
     :when-unready "Submit Gestures"})

  (reply NEXT [world]
    (when (game/all-players? world :ready)
      :select-spells))

  (event ready [world player ready?]
    (game/pset world player :ready ready?))

  (event gesture [world player {:keys [hand gesture]}]
    (let [hand (keyword hand)
          gesture (keyword gesture)]
      (cond
        ; can't change gestures without marking unready first
        (game/pget world player :ready) world
        (not (valid-gesture world player gesture)) world
        :else (game/set-gesture world player hand gesture)))))
