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
    [spellcast.phase.common :as phase-common :refer [defphase]]
    ))

(defn- enter-arena [world player]
  (log world {:player player}
    (player :name) "You advance confidently into the arena. The referee casts the formal Dispel Magic and Anti-Spell on you..."
    :else "{{name player}} strides defiantly into the arena. The referee casts the formal Dispel Magic and Anti-Spell on {{pronoun player :obj}}..."))

(defn- latest-gestures [player]
  (let [g (-> player :gestures first)]
    [(g :left) (g :right)]))

(defn- surrendered? [player]
  (= [:palm :palm] (latest-gestures player)))

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

(defphase ingame
  (reply BEGIN [world]
    (let [players (-> world :players vals)]
      (reduce enter-arena world players)))

  (reply END [world]
    (let [loser (->> (world :players) vals (filter surrendered?) first)]
      (log world {:loser loser}
        :all "The wizards engage in a game of 'first to surrender loses'. You'd be surprised how often it ends in a stalemate."
        :all "Ok, maybe you wouldn't."
        (loser :name) "<b>*** You have surrendered! ***</b>"
        :else "<b>*** {{name loser}} has surrendered! ***</b>")))
  (reply INFO [world]
    {:when-ready "Waiting for opponents..."
     :when-unready "Submit Gestures"})

  (reply NEXT [world]
    (let [loser (->> (world :players) vals (filter surrendered?) first)]
      (when loser :postgame)))

  (event gesture [world player {:keys [hand gesture]}]
    (let [hand (keyword hand)
          gesture (keyword gesture)]
      (if (valid-gesture world player gesture)
        (game/set-gesture world player hand gesture)
        world))))
