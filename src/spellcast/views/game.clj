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
       [:td#chat-ui {:rowspan 2}
        [:table
         [:tr.short [:th "GAME LOG"]]
         [:tr [:td#log]]
         [:tr.short [:td [:input#talk {:onchange "sendChatMessage(this);"}]]]]]
       [:td#status-ui
        [:table
         [:tbody
          [:tr.header [:th {:colspan 999} "GESTURES"]]
          [:tr#gesture-history]
          [:tr.header [:th {:colspan 999} "STATUS"]]
          [:tr#status-panes]]]]]
      [:tr
       [:td [:button#submit {:disabled true} "LOADING"]]]
      ]
    ]))
