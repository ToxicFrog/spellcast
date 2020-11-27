(ns spellcast.phase.pregame
  "Event handlers for pregame phase. This is the phase just after server startup, while waiting for players to join, and ends once a quorum is reached. During this phase, all players can do is talk to each other."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.phase.common :as phase :refer [dispatch-event]]
    ))

; Chat handlers common to all phases.
(defmethod dispatch-event [:pregame :log]
  [world player _ body] (phase/post-log world player body))

(defmethod dispatch-event [:pregame :INFO]
  [world _phase _event]
  (let [label (str "Waiting for players ("
                 (-> world :players count)
                 "/"
                 (-> world :settings :max-players)
                 ")")]
    {:when-ready label
     :when-unready label}))

; (defmethod dispatch [:pregame :log]
;   ([world player _] (phase-common/get-log world player))
;   ([world player _ body] (phase-common/post-log world player body)))


; (defn- check-phase-exit [world]
;   (if (= (-> world :players count) (-> world :settings :max-players))
;     (assoc world :phase :ingame)
;     world))
