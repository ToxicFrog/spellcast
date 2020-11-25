(ns spellcast.gesture-history
  (:require [reagent.dom]
            [spellcast.gesture-picker :as picker]
  ))

(defn- gesture-img [gesture hand clickable?]
  [:img {:src (str "/img/" gesture "-" hand ".png")
         :on-click (when clickable? #(picker/show-at (.-target %) hand gesture))
         :alt gesture}])

(defn- gesture-table-for-wizard [me {who :name :keys [gestures]}]
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
        (if (and (= me who) (= n (dec histsize)))
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

(defn render
  "Emit the gesture history cells for the UI. These are going to spliced into a <tr>."
  [_me _players]
  (fn [me players]
    (if (not @players)
      [:td "Error loading gesture history!"]
      (as-> @players $
            ; Make sure the current player always sorts first
            (sort-by (fn [[who _]] (if (= me who)  "" (name who))) $)
            [:<>
             (for [[_who p] $]
               ; ^{:key (str who ".gestures")}
               (gesture-table-for-wizard me p))]))))

