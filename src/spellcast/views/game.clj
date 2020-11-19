(ns spellcast.views.game
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [hiccup.page :refer [html5 include-css include-js]]
    [spellcast.world :as game]
    ))

; TODO the gesture grid is transmitted to the client when /game is fetched. This is
; wrong in at least two ways: first, the current player isn't guaranteed to be
; leftmost in the grid, and second, when another player joins everyone else needs
; to refresh the page to get the updated grid. The grid should be constructed in JS
; based on the current state of the game.
(defn gesture-table-for-player [name]
  [:table.gestures
   [:tr [:th {:colspan 2} name]]
   (for [n (reverse (range 0 8))]
     [:tr
      [:td [:img {:id (str "gesture-" name "-left-" n)
                  :src (str "/img/nothing-left.png")
                  :gesture "nothing"}]]
      [:td [:img {:id (str "gesture-" name "-right-" n)
                  :src (str "/img/nothing-right.png")
                  :gesture "nothing"}]]
      ])])

(defn gesture-tables [player]
  (let [players (->> (game/state) :players keys
                     ; make sure the current player always sorts first
                     (sort-by (fn [name] (if (= player name) ""
                                           name))))]
    [:table
     [:tr.header [:th {:colspan (count players)} "GESTURES"]]
     [:tr
      (for [p players] [:td (gesture-table-for-player p)])]]))

(defn gesture-picker [hand]
  (let [pickable-gestures ["nothing" "palm" "snap" "clap"
                           "knife" "fingers" "digit" "wave"]
        as-td (fn [gesture]
                [:td [:img {:src (str "/img/" gesture "-" hand ".png")}]])
        ]
  (as-> pickable-gestures $
        (map as-td $)
        (partition 4 $)
        (map #(conj % :tr) $)
        (mapv vec $)
        (concat [:table.gesture-picker.hidden
                 {:id (str "gesture-picker-" (name hand))}]
          $)
        (vec $))))

(defn page [_request player]
  (html5
    [:head
     [:title "Spellcast"]
     (include-js "/js/game.js")
     (include-css "/css/spellcast.css")]
    [:body {:onload (str "initSpellcast('" player "');")}
     (gesture-picker "left")
     (gesture-picker "right")
     [:table#ui
      [:tr
       [:td#chat-ui {:rowspan 3}
        [:table
         [:tr.short [:th "GAME LOG"]]
         [:tr [:td#log]]
         [:tr.short [:td [:input#talk]]]]]
       [:td#gesture-ui (gesture-tables player)]]
      [:tr
       [:td [:button#submit {:disabled true} "LOADING"]]]
      [:tr
       [:td#status-ui
        [:table
         [:tr.header [:th "STATUS"]]
         [:tr [:td#status]]]]]]
    ]))
