(ns spellcast.phase.pregame
  "Event handlers for the postgame phase. At this point the game has ended and a winner has been declared; players can continue to chat with each other if they like, but nothing else happens."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.phase.common :as phase-common :refer [dispatch-event]]
    ))

; Chat handlers common to all phases.
(defmethod dispatch-event [:postgame :log]
  [world player _ body] (phase-common/post-log world player body))

(defmethod dispatch-event [:postgame :INFO]
  ([_world _phase _event]
   {:when-ready "Game over!"
    :when-unready "Game over!"}))

; TODO: queries from the client about what the action button should say should
; return "game over" or something.
; (defmethod dispatch [:postgame :BEGIN]
;   ([world _]
;    (println "Entering postgame phase...")
;    world))

; (defmethod dispatch [:postgame :END]
;   ([world _]
;    (println "Leaving postgame phase...")
;    world))
