(ns spellcast.spellbook.basic
  "Basic spellbook for testing. Only single-gesture spells and simple effects."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.spellbook :refer [defspellbook spell-from-ns]]
    ))

(defspellbook basic
  (spell-from-ns 'spellcast.spells.surrender [:palm2])
  (spell-from-ns 'spellcast.spells.counter-spell [:snap])
  ; (spell 'magic-mirror [:wave2])
  ; (spell-from-ns 'spellcast.spells.shield [:palm])
  (spell-from-ns 'spellcast.spells.missile [:digit])
  ; (spell 'lightning-bolt [:clap2])
  ; (spell-from-ns 'spellcast.spells.stab [:knife])
  ; (spell 'paralysis [:fingers])
  )

#_ (comment
(defspellbook basic
  (spell "Surrender" [:palm2] {} ; no options, always affects caster
    (fn invoke [world {:keys [caster] :as spell}]
      (log world spell
        caster "You have surrendered!"
        :else "{{caster}} has surrendered!"))
    (fn resolve [world {:keys [caster]}]
      (assoc-in world [:players caster :hp] 0)))
  (spell "Shield" [:palm]
    {:target (option "Who do you want to cast Shield on?" living)} ; prefer-self
    default-invoke
    (fn resolve [world {:keys [target]}]
      (prn 'resolve target [:effects :shield] 0)
      (-> world
          (game/pset target [:effects :shield] 0)
          )))
  (spell "Missile" [:digit]
    {:target (option "Who do you want to cast Missile at?" living)} ; prefer-enemy
    default-invoke
    (fn resolve [world {:keys [target caster] :as spell}]
      (if (game/pget world target [:effects :shield])
        ; whoops, if caster and target are the same person this behaves badly
        ; probably want "the missile(s) strike (you/them)" a la zarfcast
        ; this requires detecting when multiple missiles are targeting the same
        ; person -- two ways I can think of to do this:
        ; - spell coalescing. Mergeable spells have a :stacks field. At invoke time
        ;   the invoke handler is run, but if the next spell in the buffer is the
        ;   same spell and it's stackable, the resolve handler is skipped and instead
        ;   (spell :stacks) is added to (next-spell :stacks). The last instance of
        ;   the spell multiplies its effect by :stacks.
        ; - effect coalescing. no spell actually has its effects in resolve; instead
        ;   it puts a pseudo-spell in the spell buffer with priority (spell+0.1)
        ;   and special handling for if an effect with that name already exists,
        ;   i.e. stacking as described above -- and the resolve handler is attached
        ;   to the effect.
        (log world spell
          caster "Your missile shatters on {{target}}'s shield."
          target "{{caster}}'s missile shatters on your shield."
          :else "{{caster}}'s missile shatters on {{target}}'s shield.")
        (-> world
            (log spell
              caster "Your missile strikes {{target}}."
              target "{{caster}}'s missile strikes you."
              :else "{{caster}}'s missile strikes {{target}}.")
            (update-in [:players target :hp] dec))))))
)
