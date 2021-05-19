(ns spellcast.spells.stab
  "flavour text goes here"
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.game :as game]
    [spellcast.data.selector :refer [living enemy]]
    [spellcast.data.spell :refer :all]
    [spellcast.logging :refer [log]]
    ))

(spell "Stab"
  :type :action
  :priority 50)

(option :target
  "Who do you want to stab?"
  :domain living
  :prefer enemy)

; Interact with other stabs: stack them together.
(interact :stab same-target?
  [self other]
  [(update self :n #(inc (or % 1)))])

(defn- log-opts [{:keys [n] :as self}]
  (if (> (or n 1) 1)
    (assoc self
      :knife "knives"
      :strikes "strike"
      :blocked "slide off")
    (assoc self
      :knife "knife"
      :strikes "strikes"
      :blocked "slides off")))

(defn invoke :- Game
  [world :- Game, {:keys [caster hand target] :as spell} :- Spell]
  (cond
    (= :nothing target)
    (log world (assoc spell :hand (name hand))
      caster "You stab at the air with the knife in your {{hand}} hand, ineffectually."
      :else "{{caster}} waves the knife in {{their caster}} {{hand}} hand aimlessly.")
    :else
    (log world (assoc spell :hand (name hand))
      caster "You stab at {{target}} (with the {{hand}} hand)."
      target "{{caster}} stabs at you (with the {{hand}} hand)."
      :else "{{caster}} stabs at {{target}} (with the {{hand}} hand).")))

(defn finalize :- Game
  [world :- Game, {:keys [target caster] :as self} :- Spell]
  (cond
    (= :nothing target) world
    (:shielded self)
    (-> world
        (log (log-opts self)
          target "The {{knife}} {{blocked}} your shield."
          :else "The {{knife}} {{blocked}} {{target}}'s shield."))
    :else
    (-> world
        (log (log-opts self)
          target "The {{knife}} {{strikes}} you."
          :else "The {{knife}} {{strikes}} {{target}}.")
        (game/pupdate target :hp dec))))
