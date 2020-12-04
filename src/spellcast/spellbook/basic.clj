(ns spellcast.spellbook.basic
  "Basic spellbook for testing. Only single-gesture spells and simple effects."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.game :as game]
    [spellcast.data.spellbook :refer [defspellbook spell option]]
    [spellcast.logging :refer [log]]
    [spellcast.spellbook.common :refer [default-invoke]]
    ))

(defn living [world spell]
  (->> world
       :players
       (filter (fn [[k v]] (> (:hp v) 0)))
       (map first)))

(defn self [world spell]
  (:caster spell))

(defn enemy [spell world]
  (-> world
      :players
      keys
      set
      (disj (:caster spell))
      first))

(defspellbook basic
  (spell "Surrender" [:palm2] {}
    (fn invoke [world {:keys [caster] :as spell}]
      (log world spell
        caster "You have surrendered!"
        :else "{{caster}} has surrendered!"))
    (fn resolve [world {:keys [caster]}]
      (assoc-in world [:players caster :hp] 0)))
  (spell "Shield" [:palm]
    {:target (option "Who do you want to cast Shield on?" living self)}
    default-invoke
    (fn resolve [world {:keys [target]}]
      (prn 'resolve target [:effects :shield] 0)
      (-> world
          (game/pset target [:effects :shield] 0)
          )))
  (spell "Missile" [:digit]
    {:target (option "Who do you want to cast Missile at?" living enemy)}
    default-invoke
    (fn resolve [world {:keys [target caster] :as spell}]
      (if (game/pget world target [:effects :shield])
        (log world spell
          caster "Your missile shatters on {{target}}'s shield."
          target "{{caster}}'s missile shatters on your shield."
          :else "{{caster}}'s missile shatters on {{target}}'s shield.")
        (-> world
            (log spell
              caster "Your missile strikes {{target}}."
              target "{{caster}}'s missile strikes you."
              :else "{{caster}}'s missile strikes {{target}}.")
            (update-in world [:players target :hp] dec))))))
