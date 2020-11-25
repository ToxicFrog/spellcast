(ns spellcast.gesture-picker
  "Gesture picker elements, for players to select gestures for each turn.

  Use init first to weave it into the DOM, then show or show-at to display it; it will pop up at the specified coordinates, wait for a gesture to be selected, then send it to the server and hide itself."
  (:require [reagent.core :as r]
            [reagent.dom]
  ))

(def ^:private picker-location
  "The control atom for the gesture picker. When nil the rendering function itself returns nil and is thus ignored by Reagent. When set, it stores the arguments to (show) for reading by the rendering function; when interaction with the picker is complete, the click handler clears this atom and causes it to vanish again."
  (r/atom nil))

(defn show
  "Display the gesture picker at the given coordinates. The specified hand will be used for display and for sending the selected gesture to the server; the specified gesture, if not nil, will be used to highlight the currently-selected gesture."
  [x y hand gesture]
  (reset! picker-location [x y hand gesture]))

(defn show-at
  "As (show) but derives the display coordinates from the position of the parent element instead."
  [parent hand gesture]
  (let [rect (.getBoundingClientRect parent)
        x (.-left rect)
        y (.-top rect)]
    (show x y hand gesture)))

(defn- pick-gesture [hand gesture]
  (println "gesture picked!" hand gesture)
  ; TODO: need to POST this to the server
  (reset! picker-location nil))

(defn- gesture-picker
  "The actual rendering code for the gesture picker. If location-atom is nil, does nothing; otherwise returns a 4x2 table of pickable gestures with onclick handlers."
  [location-atom]
  (when-let [location @location-atom]
    (let [[x y hand old-gesture] location
          pickable-gestures [["nothing" "palm" "snap" "clap"]
                             ["knife" "fingers" "digit" "wave"]]]
    [:table.gesture-picker {:style {:left (str x "px") :top (str y "px")}}
     [:tbody
     (for [line pickable-gestures]
       ^{:key (apply str "gesture-picker-row-" line)}
       [:tr
        (for [gesture line]
          ^{:key (str "gesture-picker-cell-" gesture)}
          [:td (when (= gesture old-gesture) {:class "selected-gesture"})
           [:img {:src (str "/img/" gesture "-" hand ".png")
                      :on-click #(pick-gesture hand gesture)}]])
        ])
     ]])))

(defn init
  "Weave the gesture picker into the DOM, probably in a <div> tucked out of the way somewhere.
  You must call this before show or show-at or those functions will have no effect."
  [parent]
  (reagent.dom/render [gesture-picker picker-location] parent))
