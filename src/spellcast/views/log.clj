(ns spellcast.views.log
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [ring.util.response :as r]
    [spellcast.data.game :as game]
    [spellcast.data.player :refer [Player]]
    [spellcast.world :as world]
    [spellcast.phase.common :as phase]
    ))

(defn long-poll
  "Watch the game state (using world/watch) until (f world) returns something with a different stamp, then return that as JSON with an appropriate X-Stamp header."
  [f stamp]
  (let [data (world/watch f stamp)
        stamp' (hash data)]
    (-> (r/response data)
        (r/content-type "application/json")
        (r/header "X-Stamp" stamp'))))

(defn log [player stamp]
  (long-poll #(game/get-log % player) stamp))

(defn players
  "Get the player state, as visible to the named player.
  In particular, this means that the :gestures table for each player is filtered according to what the querent is allowed to see."
  [player stamp]
  (long-poll #(game/get-filtered-players % player) stamp))

(defn- ready-info
  "Get the full structure describing player readiness info from the world, for use with watch."
  [world player]
  (assoc (phase/phase-info world)
    :ready (-> world (game/get-player player) :ready)))

(defn ready
  "Get information about the player's readiness state, and what text should be displayed for the ready/unready states."
  [player stamp]
  (when player
    (long-poll #(ready-info % player) stamp)))

(def page log)
