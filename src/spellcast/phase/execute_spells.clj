(ns spellcast.phase.execute-spells
  "Execute the spells buffered by select-spells."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.game :as game]
    [spellcast.data.player :as player]
    [spellcast.phase.common :as phase-common :refer [defphase]]
    ))

(defn- execute-next-spell
  "Pop the first spell from the execution queue and cast it by calling its :invoke and :resolve handlers. Returns the new world state that results."
  [world]
  (let [queue (:casting world)
        {:keys [invoke resolve] :as spell} (first queue)
        world (assoc world :casting (rest queue))]
    (info "Executing spell: " (:name spell))
    (-> world
        (invoke spell)
        (resolve spell))))

(defphase execute-spells
  (reply BEGIN [world]
    (info "Beginning spell execution...")
    (->> world
         (iterate execute-next-spell)
         (drop-while (comp not-empty :casting))
         first))
  (reply END [world]
    (pprint world)
    world)
  (reply INFO [world]
    {:when-ready "Processing..."
     :when-unready "Processing..."})
  (reply NEXT [world]
    :cleanup))
