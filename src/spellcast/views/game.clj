(ns spellcast.views.game
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.form :as form]
            [ring.util.response :as r]
            [spellcast.game :as game]
            ))

(defn page [{:keys [:session] :as request}]
  (html5
    [:head
     [:title "Spellcast"]
     (include-js "/js/game.js")
     (include-css "/css/screen.css")]
    [:body {:onload "initSpellcast();"}
     [:div#log]
     [:input#talk]
     ]))
     ; [:table
     ;  [:tr
     ;   [:td {:width "120em"}
     ;    (seq (interpose [:br] (game/log)))]
     ;   [:td ...players...]]]]
; )
;    (-> (str
;          (text/msg "{{get p1 :name}} strides confidently into the arena. The referee casts the formal Dispel Magic and Anti-Magic on {{prn p1 :obj}}..."
;                    :p1 (game/player (session :name))
;          "\n\n"
;          request)
;        (r/response)
;        (r/content-type "text/plain")
;        (assoc :session session))))
