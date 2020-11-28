(ns spellcast.phase.pregame
  "Event handlers for the postgame phase. At this point the game has ended and a winner has been declared; players can continue to chat with each other if they like, but nothing else happens."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.phase.common :as phase-common :refer [defphase]]
    ))

(defphase postgame
  (reply BEGIN [world] world)
  (reply END [world] world)
  (reply NEXT [_] nil)
  (reply INFO [_world]
    {:when-ready "Game over!"
     :when-unready "Game over!"}))
