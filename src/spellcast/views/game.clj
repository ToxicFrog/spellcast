(ns spellcast.views.game
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
  (:require
    [hiccup.page :refer [html5 include-css include-js]]
    [spellcast.world :as game]
    ))

(comment
  "
  TODO: how the fuck do I do this in CSS?
  ------------------------------
  |    |    |    |    |    |   |
  |    |gest|    |    |    |   |
  |    |ures|    |    |  <--------- subtable for gestures
  |log |    |    |    |    |   |
  |    |-----------------------|
  |----|  questions      | stat|
  |chat|             ^   |  us |
  -------------------|----------
                     |subtable for questions
  ")

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
      [:td [:img {:id (str "gesture:" name ":L:" n)
                      :src "/img/nothing-left.png"}]]
      [:td [:img {:id (str "gesture:" name ":R:" n)
                      :src "/img/nothing-right.png"}]]
      ])])

(defn gesture-tables []
  (let [players (-> (game/state) :players keys)]
    [:table
     [:tr.header [:th {:colspan (count players)} "GESTURES"]]
     [:tr
      (for [p players] [:td (gesture-table-for-player p)])]]))

(defn page [_request _player]
  (html5
    [:head
     [:title "Spellcast"]
     (include-js "/js/game.js")
     (include-css "/css/spellcast.css")]
    [:body {:onload "initSpellcast();"}
     [:table#ui
      [:tr
       [:td#chat-ui {:rowspan 3}
        [:table
         [:tr.header [:th "GAME LOG"]]
         [:tr [:td [:div#log [:input#log-index {:type :hidden :value 0}]]]]
         [:tr [:td [:input#talk]]]]]
       [:td#gesture-ui (gesture-tables)]]
      [:tr
       [:td [:button#submit {:disabled true} "LOADING"]]]
      [:tr
       [:td#status-ui
        [:table
         [:tr.header [:th "STATUS"]]
         [:tr [:td#status]]]]]]
    ]))
