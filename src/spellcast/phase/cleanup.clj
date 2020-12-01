(ns spellcast.phase.cleanup
  ""
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.phase.common :as phase-common :refer [defphase]]
    [spellcast.data.game :as game]
    ))

(defn- living? [player]
  (> (get player :hp 0) 0))

(defphase cleanup
  (reply BEGIN [world]
    ; tick end-of-turn effects including stuff like buffs expiring, poison/disease
    ; counting down, etc.
    world)
  (reply END [world] world)
  (reply NEXT [world]
    (let [living-players (game/filter-players world living?)]
      (if (> (count living-players) 1)
        :collect-gestures
        :postgame)))
  (reply INFO [_world]
    {:when-ready "End of turn..."
     :when-unready "End of turn..."}))
