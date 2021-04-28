(ns spellcast.phase.execute-spells
  "Execute the spells buffered by select-spells."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.game :as game :refer [Game]]
    [spellcast.data.spell :refer [Spell]]
    [spellcast.logging :refer [log]]
    [spellcast.phase.common :as phase-common :refer [defphase]]
    ))

"This needs complete rethinking. The new spell execution flow is something like:
- put all spells in the execution queue, logging 'x casts y' as we do so
- sort by priority
- foreach spell, check for interactions against all spells after it in the queue, which is to say:
  - let [spell tail] (head queue) (rest queue)
  - reduce spell interactions over tail, producing final spell and next-queue
  - put resulting spell state into final-queue, and repeat process with queue <- next-queue
- execute resulting queue of fully-interacted spell effects"

(defn- interact-spells
  [queue]
  (loop [head (first queue)
         tail (rest queue)
         done []]

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

(defn invoke-spell [world spell]
  (let [invoke (get spell :invoke)]
    ((:invoke spell) world spell)))

(defphase execute-spells
  (reply BEGIN [world]
    (info "Beginning spell execution...")
    ; possibly we should move this into the end of select-spells?
    (reduce invoke-spell world (:casting world)))
    ; (->> world
    ;      ; We use this instead of just a reduce across (world :casting) because
    ;      ; executing a spell may edit the spell queue.
    ;      (iterate execute-next-spell)
    ;      (drop-while (comp not-empty :casting))
    ;      first))
  (reply END [world]
    (pprint world)
    world)
  (reply INFO [world]
    {:when-ready "Processing..."
     :when-unready "Processing..."})
  (reply NEXT [world]
    :cleanup))
