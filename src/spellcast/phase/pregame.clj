(ns spellcast.phase.pregame
  "Event handlers for pregame phase. This is the phase just after server startup, while waiting for players to join, and ends once a quorum is reached. During this phase, all players can do is talk to each other."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.logging :refer [log]]
    [spellcast.phase.common :as phase :refer [defphase]]
    ))

(defn- enter-arena [world player]
  (log world {:player player}
    (player :name) "You advance confidently into the arena. The referee casts the formal Dispel Magic and Anti-Spell on you..."
    :else "{{name player}} strides defiantly into the arena. The referee casts the formal Dispel Magic and Anti-Spell on {{pronoun player :obj}}..."))

(defphase :pregame
  (reply BEGIN [world] world)
  (reply END [world]
    (let [players (-> world :players vals)]
      (reduce enter-arena world players)))
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
      :collect-gestures)))
