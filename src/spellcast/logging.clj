(ns spellcast.logging
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
  (:require
    [antlers.core :refer [render-string]]
    [spellcast.state.game :refer [Game add-log]]
    ))

(def pronouns-map
  {:they {:sub "they" :obj "them" :pos "their" :ref "themself"}
   :she  {:sub  "she" :obj "her"  :pos "her"   :ref "herself"}
   :he   {:sub  "he"  :obj "him"  :pos "his"   :ref "himself"}
   :it   {:sub  "it"  :obj "it"   :pos "its"   :ref "itself"}})

(defn- pronoun [player context]
  (get-in pronouns-map [(player :pronouns) context]))

(defn- render-msg [bindings text]
  (render-string
    text
    (merge {:pronoun pronoun
            :name #(get % :name)}
      bindings)))

(defn log :- Game
  "Log one or more messages. `bindings` is a map of bindings to be used with (msg) to render each message (these bindings are common to all messages in this call to log). `rest` is a sequence of `name message` pairs. Each message will be logged to the named player. You can also use :all to log to all players, or :else to log to all players not explicitly mentioned in this call."
  [world :- Game, bindings :- {s/Keyword s/Any}, & rest]
  (let [messages (partition 2 rest)
        names (-> (map first messages) (set) (disj :all :else))
        else (complement names)
        log-one (fn log-one [world [dst msg]]
                  (cond
                    (set? dst) (add-log world msg dst)
                    (string? dst) (add-log world msg #{dst})
                    (= :else dst) (add-log world msg else)
                    (= :all dst) (add-log world msg (constantly true))))]
    (->> messages
         (map (fn [[dst msg]] [dst (render-msg bindings msg)]))
         (reduce log-one world))))

 ; ; Example usage
 ; (log {:p1 caster :p2 target :hands "the left hand"}
 ;   :all "{{p1 :name}} casts Magic Missile (with {{hands}}) at {{p2 :name}}."
 ;   caster "Your missile shatters on {{p2 :name}}'s shield."
 ;   target "The missile shatters on your shield."
 ;   :else "The missile shatters on {{p2 :name}}'s shield.")
