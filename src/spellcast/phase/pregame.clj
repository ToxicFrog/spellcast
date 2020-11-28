(ns spellcast.phase.pregame
  "Event handlers for pregame phase. This is the phase just after server startup, while waiting for players to join, and ends once a quorum is reached. During this phase, all players can do is talk to each other."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.phase.common :as phase :refer [defphase]]
    ))

(defphase :pregame
  (reply BEGIN [world] world)
  (reply END [world] world)
  (reply INFO [world]
    (let [label (str "Waiting for players ("
                  (-> world :players count)
                  "/"
                  (-> world :settings :max-players)
                  ")")]
      {:when-ready label
       :when-unready label}))

  (reply NEXT [world]
    (when (= (-> world :settings :max-players) (-> world :players count))
      :ingame)))
