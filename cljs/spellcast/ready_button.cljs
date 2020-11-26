(ns spellcast.ready-button
  "The button used to signal readiness -- once a player has selected gestures or answers, they click this to indicate that they are ready for play to proceed, and click again if they changed their mind.
  The text on the button gives them some idea what the game is waiting for."
  (:require [reagent.core :as r]
            [reagent.dom]
            [spellcast.net :as net]
  ))

(def ^:private button-info
  "The control atom for the ready button. Holds information about:
  - whether or not the player is ready
  - what text to display when they are or are not ready"
  (net/poll "/data/phase-info"))

(defn send-ready [ready?]
  (net/post "/game/ready" ready?))

(defn ready-button [info]
  (if-let [{:keys [is-ready when-ready when-unready]} @info]
    (if is-ready
      [:button.ready {:on-click #(send-ready false)} when-ready]
      [:button.unready {:on-click #(send-ready true)} when-unready])
    [:button.error {:disabled true} "ERROR"]))

(defn init
  "Weave the gesture picker into the DOM, probably in a <div> tucked out of the way somewhere.
  You must call this before show or show-at or those functions will have no effect."
  [parent]
  (reagent.dom/render [ready-button button-info] parent))
