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
    [spellcast.data.game :as game]
    [spellcast.logging :refer [log]]
    [spellcast.phase.common :as phase-common :refer [dispatch-event]]
    ))

; Chat handlers common to all phases.
(defmethod dispatch-event [:ingame :log]
  [world player _request body] (phase-common/post-log world player body))

(defn- enter-arena [world player]
  (log world {:player player}
    (player :name) "You advance confidently into the arena. The referee casts the formal Dispel Magic and Anti-Spell on you..."
    :else "{{name player}} strides defiantly into the arena. The referee casts the formal Dispel Magic and Anti-Spell on {{pronoun player :obj}}..."))

(defn- game-start [world]
  (let [players (-> world :players vals)]
    (reduce enter-arena world players)))

(defn- latest-gestures [player]
  (let [g (-> player :gestures first)]
    [(g :left) (g :right)]))

(defn- surrendered? [player]
  (= [:palm :palm] (latest-gestures player)))

(defn- test-game-resolution [world]
  ; (pprint world)
  (let [loser (->> (world :players) vals (filter surrendered?) first)]
    (if loser
      (-> world
          (log {:loser loser}
            :all "The wizards engage in a game of 'first to surrender loses'. You'd be surprised how often it ends in a stalemate."
            :all "Ok, maybe you wouldn't."
            (loser :name) "<b>*** You have surrendered! ***</b>"
            :else "<b>*** {{name loser}} has surrendered! ***</b>")
          (assoc :phase :postgame))
      world)))

(defmethod dispatch-event [:ingame :BEGIN]
  ([world _phase _event] world))

(defmethod dispatch-event [:ingame :END]
  ([world _phase _event] world))

(defn- valid-gesture
  "Perform validation on an attempted gesture set request. The rules are:
  - no setting of internal gestures (:antispell or :unseen)
  - no setting both hands to :knife"
  [world player gesture]
  (let [has-knife? (-> (game/get-gestures world player) set :knife)]
    (cond
      (#{:antispell :unseen} gesture) false
      (and has-knife? (= :knife gesture)) false
      :else true)))

(defmethod dispatch-event [:ingame :gesture]
  [world player _request {:keys [hand gesture]}]
  (let [hand (keyword hand)
        gesture (keyword gesture)]
    (if (valid-gesture world player gesture)
      (as-> world $
            (game/set-gesture $ player hand gesture)
            (test-game-resolution $))
      world)))
