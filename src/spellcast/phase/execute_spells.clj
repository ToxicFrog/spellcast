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
    ; [spellcast.logging :refer [log]]
    [spellcast.phase.common :as phase-common :refer [defphase]]
    ))

"This needs complete rethinking. The new spell execution flow is something like:
- put all spells in the execution queue, logging 'x casts y' as we do so
- sort by priority
- foreach spell, check for interactions against all spells after it in the queue, which is to say:
  - let [spell tail] (head queue) (rest queue)
  - reduce spell interactions over tail, producing final spell and next-queue
  - put resulting spell state into final-queue, and repeat process with queue <- next-queue
- execute resulting queue of fully-interacted spell effects

Ok so let's break down spell interaction in more detail
- outQ <- []
- while inQ is non-empty
 - pull the next spell S from the inQ
 - inQ' <- []
 - for each remaining spell R in the inQ
  - for each interaction I on S
   - if (I S R) returns non-nil, bind to [S tail...] and append tail to inQ'
   - if it returns nil, append R to inQ'
 - append S to outQ
 - inQ <- inQ'
"

(defn- interact-spell-pair
  "Check every interaction in spell against other-spell. Return [spell-with-any-modifications out-queue-with-interaction-results-applied]."
  [[spell out-queue] other-spell]
  ; check every interaction in spell against other-spell
  ; possible wrinkle: an interaction might result in other-spell expanding to
  ; multiple spells
  ; if so we need to check all the remaining interactions against all of those...
  (let [interactions (:interactions spell)]
    (info "Checking interactions between" (:name spell) "and" (:name other-spell))
    (info "Interaction list:" interactions)
    (as-> interactions $
          ; Run every interaction in this spell across the other-spell
          (map #(% spell other-spell) $)
          ; Take the first one that matched
          (some identity $)
          (if $
            [(first $) (concat out-queue (rest $))]
            ; No interactions matched? Just push the other-spell into the out-queue
            [spell (conj out-queue other-spell)]))))

(defn- interact-next-spell
  "Given a spell or spell-like effect and a queue of spell effects of lower priority, interact this spell with all of them, returning a vector of [final-state-of-the-spell queue-with-interactions-applied]."
  [spell queue]
  (reduce interact-spell-pair [spell []] queue))

(defn- interact-spells
  "Given a queue of spells, do interaction checks, by checking the first spell in the queue against all subsequent spells, then the second, etc, until the entire queue has been processed. Note that interaction checks can rewrite the remaining queue contents. Returns the fully interacted spell queue."
  [queue]
  (loop [head (first queue)
         tail (rest queue)
         done []]
    (if head
      (let [[head tail] (interact-next-spell head tail)]
        (recur
          (first tail)
          (rest tail)
          (conj done head)))
      done)))

; (defn- execute-next-spell
;   "Pop the first spell from the execution queue and cast it by calling its :invoke and :finalize handlers. Returns the new world state that results."
;   [world]
;   (let [queue (:casting world)
;         {:keys [invoke finalize] :as spell} (first queue)
;         world (assoc world :casting (rest queue))]
;     (info "Executing spell: " (:name spell))
;     (-> world
;         (invoke spell)
;         (finalize spell))))

(defn invoke-spell [world :- Game, spell :- Spell]
  (info "Invoking " (:name spell))
  (let [invoke-fn (:invoke spell)]
    (invoke-fn world spell)))

(defn finalize-spell [world :- Game, spell :- Spell]
  (info "Resolving " (:name spell))
  (let [finalize-fn (:finalize spell)]
    (finalize-fn world spell)))

(defphase execute-spells
  (reply BEGIN [world]
    (info "Emitting spell invokation messages...")
    (let [world' (reduce invoke-spell world (:casting world))
          _ (info "Resolving spell interactions...")
          spells (interact-spells (:casting world))]
      (info "Finalizing spell effects...")
      (reduce finalize-spell world' spells)))
    ; possibly we should move this into the end of select-spells?
    ; (reduce invoke-spell world (:casting world)))
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
