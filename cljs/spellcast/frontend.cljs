(ns spellcast.frontend
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! go]]
            [oops.core :refer [oget oset! ocall!]]
            [reagent.core :as r]
            [reagent.dom]
            [spellcast.gesture-picker :as picker]
  ))

(enable-console-print!)

(defn $ [& sel] (.querySelector js/document (apply str sel)))
(defn $$ [& sel] (.querySelectorAll js/document (apply str sel)))

(def me nil)
(defn me? [who] (= who me))

(defn long-poll
  "Long-poll the given view path and whenever it changes, reset! the result into the atom. If called without a stamp, uses a stamp of 0; subsequent refreshes get the stamp from the X-Stamp header."
  ([path atom] (long-poll path atom 0))
  ([path atom stamp]
    (go
      (let [url (str path "/" stamp)
            response (<! (http/get url {:with-credentials? true}))
            success (= (response :status) 200)]
        (println ">> " url "\n" response)
        (if success
          (do
            (reset! atom (response :body))
            (js/setTimeout long-poll 100 path atom (get-in response [:headers "x-stamp"])))
          (do
            (reset! atom nil)
            (js/setTimeout long-poll 5000 path atom 0)))))))

(defn log-view []
  (let [log (r/atom [])]
    (long-poll "/data/log" log)
    (fn []
      (if @log
        (into [:div] (interpose [:br] @log))
        [:div "Error loading log!"]))))

(defn- gesture-img [gesture hand clickable?]
  [:img {:src (str "/img/" gesture "-" hand ".png")
         :on-click (when clickable? #(picker/show-at (.-target %) hand gesture))
         :alt gesture}])

(defn gesture-table-for-player [{who :name :keys [gestures]}]
  (let [histsize 8
        filler (repeat {:left "nothing" :right "nothing"})
        gestures (->> (lazy-cat gestures filler)
                      (take histsize)
                      reverse
                      (map-indexed vector))]
    [:td
     [:table.gesture-history
      [:tbody
       ; ^{:key (str who "-gestures-0")}
       [:tr [:th {:col-span 2} who]]
       (for [[n gesture] gestures]
        ; ^{:key (str who "-gestures-" n)}
        (if (and (me? who) (= n (dec histsize)))
          ; last row of the history table and it's the history table for the current player
          ; TODO this is ugly, clean it up somehow
          [:tr
           [:td.pickable-gesture (gesture-img (gesture :left) "left" true)]
           [:td.pickable-gesture (gesture-img (gesture :right) "right" true)]]
          ; otherwise, not last row and/or gestures for other players
          [:tr
           [:td (gesture-img (gesture :left) "left" false)]
           [:td (gesture-img (gesture :right) "right" false)]]))
       ]]]))

(def gesture-table-for-wizard gesture-table-for-player)

(defn status-pane-for-wizard [{who :name :keys [hp effects]}]
  (let [health-class (condp >= hp
                       5  "hp critical"
                       10 "hp low"
                       14 "hp damaged"
                       "hp full")]
    [:td.status
     [:pre "Health: " hp
      (when (me? who) [:span {:class health-class} " ❤"])
      "\n"
      (for [[effect duration] effects]
        (str duration " " (name effect) "\n"))
      ]]))

(defn gesture-history
  "Emit the gesture history cells for the UI. These are going to spliced into a <tr>."
  [_players]
  (fn [players]
    (if (not @players)
      [:td "Error loading gesture history!"]
      (as-> @players $
            ; Make sure the current player always sorts first
            (sort-by (fn [[who _]] (if (me? who)  "" (name who))) $)
            [:<>
             (for [[_who p] $]
               ; ^{:key (str who ".gestures")}
               (gesture-table-for-wizard p))]))))

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
             (for [[_who p] $]
               ; ^{:key (str who ".statline")}
               (status-pane-for-wizard p))]))))

(defn post [path body]
  (http/post path {:with-credentials? true
                   :json-params body}))

(defn send-chat-message [element]
  (post "/game/log" {:text (oget element :value)})
  (oset! element :value ""))

(defn init [player]
  (set! me player)
  (reagent.dom/render [log-view] ($ "#log"))
  (picker/init ($ "#gesture-picker"))
  ; (reagent.dom/render [gesture-picker "left" "nothing"] ($ "#gesture-picker"))
  (let [players (r/atom {})]
    (long-poll "/data/players" players)
    (reagent.dom/render [gesture-history players] ($ "#gesture-history"))
    (reagent.dom/render [status-panes players] ($ "#status-panes"))))

(set! js/initSpellcast init)
(set! js/sendChatMessage send-chat-message)
