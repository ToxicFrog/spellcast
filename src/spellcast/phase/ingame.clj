(ns spellcast.phase.ingame
  "Event handlers for ingame phase. This is the phase after all players have joined, where the actual dueling happens. It will probably eventually be split into multiple subphases, e.g. collect-gestures -> collect-answers -> execute.
  At the moment, it's just a test phase, and arbitrarily picks and player to win and then goes on to the postgame."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
  (:require
    [clojure.string :as string]
    [ring.util.response :as r]
    [spellcast.logging :refer [log]]
    [spellcast.phase.common :as phase-common :refer [dispatch]]
    ))

; Chat handlers common to all phases.
(defmethod dispatch [:ingame :log]
  ([world player _] (phase-common/get-log world player))
  ([world player _ body] (phase-common/post-log world player body)))

