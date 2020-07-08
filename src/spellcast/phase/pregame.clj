(ns spellcast.phase.pregame
  "Event handlers for pregame phase. This is the phase just after server startup, while waiting for players to join, and ends once a quorum is reached. During this phase, all players can do is talk to each other."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [ring.util.response :as r]
    [spellcast.data.game :as game]
    [spellcast.data.player :as player :refer [->Player]]
    [spellcast.logging :refer [log]]
    [spellcast.phase.common :as phase-common :refer [dispatch-event]]
    [spellcast.views.join :as views.join]
    ))

; Chat handlers common to all phases.
(defmethod dispatch-event [:pregame :log]
  [world player _ body] (phase-common/post-log world player body))

; (defmethod dispatch [:pregame :log]
;   ([world player _] (phase-common/get-log world player))
;   ([world player _ body] (phase-common/post-log world player body)))


; (defn- check-phase-exit [world]
;   (if (= (-> world :players count) (-> world :settings :max-players))
;     (assoc world :phase :ingame)
;     world))

; (defmethod POST [:pregame :BEGIN]
;   ([world _]
;    (println "Entering pregame phase...")
;    world))

; (defmethod dispatch [:pregame :END]
;   ([world _]
;    (println "Leaving pregame phase...")
;    world))
