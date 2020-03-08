(ns spellcast.views.game
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.form :as form]
            [ring.util.response :as r]
            [spellcast.game :as game]
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

(defn page [{:keys [:session] :as request}]
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
         [:tr [:td [:div#log]]]
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