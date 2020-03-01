(ns spellcast.logging
  (:require [antlers.core :refer [render-string]]
            [spellcast.game :as game]
            [clojure.set :refer [difference]]))

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

(defn log
  "Log one or more messages. `bindings` is a map of bindings to be used with (msg) to render each message (these bindings are common to all messages in this call to log). `rest` is a sequence of `name message` pairs. Each message will be logged to the named player. You can also use :all to log to all players, or :else to log to all players not explicitly mentioned in this call.
  log is non-transactional (i.e. there is no guarantee log messages won't be interleaved with messages from other calls) but the messages are guaranteed to be logged in the order they are provided."
  [bindings & rest]
  (let [messages (partition 2 rest)
        names (-> (map first messages) (set) (disj :all :else))
        else (complement names)
        ]
    (->> messages
         (map (fn [[dst msg]] [dst (render-msg bindings msg)]))
         (map (fn [[dst msg]]
                (cond
                  (set? dst) (game/log! msg dst)
                  (string? dst) (game/log! msg #{dst})
                  (= :else dst) (game/log! msg else)
                  (= :all dst) (game/log! msg (constantly true)))))
         dorun)))

(comment
  ; Example usage
  (log {:p1 caster :p2 target :hands "the left hand"}
    :all "{{p1 :name}} casts Magic Missile (with {{hands}}) at {{p2 :name}}."
    caster "Your missile shatters on {{p2 :name}}'s shield."
    target "The missile shatters on your shield."
    :else "The missile shatters on {{p2 :name}}'s shield.")
  )
