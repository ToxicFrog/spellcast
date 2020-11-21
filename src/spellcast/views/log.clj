(ns spellcast.views.log
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [hiccup.core :refer [html]]
    [ring.util.response :as r]
    [spellcast.data.game :as game]
    [spellcast.world :as world]
    ))

(defn long-poll
  "Watch the game state (using world/watch) until (f world) returns something with a different stamp, then return that as JSON with an appropriate X-Stamp header."
  [f stamp]
  (as-> (world/watch f stamp) $
        (r/response $)
        (r/content-type $ "application/json")
        (r/header $ "X-Stamp" (hash $))
        $))

(defn log [player stamp]
  (long-poll #(game/get-log % player) stamp))

(defn players
  "Get the player state, as visible to the named player.
  In particular, this means that the :gestures table for each player is filtered according to what the querent is allowed to see.
  This con"
  [player stamp]
  (long-poll #(game/get-filtered-players % player) stamp))

(def page log)
