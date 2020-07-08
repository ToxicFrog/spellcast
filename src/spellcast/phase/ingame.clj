(ns spellcast.phase.ingame
  "Event handlers for ingame phase. This is the phase after all players have joined, where the actual dueling happens. It will probably eventually be split into multiple subphases, e.g. collect-gestures -> collect-answers -> execute.
  At the moment, it's just a test phase, and arbitrarily picks and player to win and then goes on to the postgame."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [clojure.string :as string]
    [ring.util.response :as r]
    [spellcast.logging :refer [log]]
    [spellcast.phase.common :as phase-common :refer [dispatch-event]]
    ))

; Chat handlers common to all phases.
(defmethod dispatch-event [:ingame :log]
  [world player _ body] (phase-common/post-log world player body))

(defn- enter-arena [world player]
  (log world {:player player}
    (player :name) "You advance confidently into the arena. The referee casts the formal Dispel Magic and Anti-Spell on you..."
    :else "{{name player}} strides defiantly into the arena. The referee casts the formal Dispel Magic and Anti-Spell on {{pronoun player :obj}}..."))

(defn- game-start [world]
  (let [players (-> world :players vals)]
    (reduce enter-arena world players)))

(defn- test-game-resolution [world]
  ; (pprint world)
  (let [players (world :players)
        n (rand-int (count players))
        [_ winner] (-> players seq (nth n))]
    (-> world
        (log {:winner winner}
          :all "The duel begins! It is very impressive, but you can neither see nor participate in it, because that part of the server isn't implemented yet."
          :all "When the dust clears, however, only one wizard is left standing..."
          (winner :name) "<b>*** You are victorious! ***</b>"
          :else "<b>*** {{name winner}} is victorious! ***</b>")
        (assoc :phase :postgame))))

(defmethod dispatch-event [:ingame :BEGIN]
  ([world _phase _event]
   (test-game-resolution (game-start world))))

(defmethod dispatch-event [:ingame :END]
  ([world _phase _event]
   world))
