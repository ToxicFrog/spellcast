(ns spellcast.frontend
  (:require [oops.core :refer [oget oset! ocall!]]
            [reagent.dom]
            [spellcast.gesture-history :as history]
            [spellcast.gesture-picker :as picker]
            [spellcast.net :as net]
  ))

(enable-console-print!)

(defn $ [& sel] (.querySelector js/document (apply str sel)))
(defn $$ [& sel] (.querySelectorAll js/document (apply str sel)))

(def me nil)
(defn me? [who] (= who me))

(defn log-view []
  (let [log (net/poll "/data/log")]
    (fn []
      (if @log
        (into [:div] (interpose [:br] @log))
        [:div "Error loading log!"]))))

(defn send-chat-message [element]
  (net/post "/game/log" {:text (oget element :value)})
  (oset! element :value ""))

(defn status-pane-for-wizard [{who :name :keys [hp effects]}]
  (let [health-class (condp >= hp
                       5  "hp critical"
                       10 "hp low"
                       14 "hp damaged"
                       "hp full")]
    ^{:key (str "status-pane." who)}
    [:td.status
     [:pre "Health: " hp
      (when (me? who) [:span {:class health-class} " ❤"])
      "\n"
      (for [[effect duration] effects]
        (str duration " " (name effect) "\n"))
      ]]))

(defn status-panes
  "Emit the status pane cells for the UI. As with gesture-history, these get spliced into a <tr> and thus should be <td>s."
  [_players]
  (fn [players]
    (if (not @players)
      [:td "Error loading player status"]
      (as-> @players $
            ; Make sure the current player always sorts first
            (sort-by (fn [[who _]] (if (me? who)  "" (name who))) $)
            [:<>
             (for [[who p] $]
               ^{:key (str "status-pane." who)}
               (status-pane-for-wizard p))]))))

(defn init [player]
  (set! me player)
  (reagent.dom/render [log-view] ($ "#log"))
  (picker/init ($ "#gesture-picker"))
  ; (reagent.dom/render [gesture-picker "left" "nothing"] ($ "#gesture-picker"))
  (let [players (net/poll "/data/players")]
    (reagent.dom/render [history/render player players] ($ "#gesture-history"))
    (reagent.dom/render [status-panes players] ($ "#status-panes"))))

(set! js/initSpellcast init)
(set! js/sendChatMessage send-chat-message)

