(ns spellcast.views.join
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [hiccup.form :as form]
    [hiccup.page :refer [html5 include-css]]
    [ring.util.response :as r]
    [slingshot.slingshot :refer [try+ throw+]]
    [spellcast.data.game :as game]
    [spellcast.data.player :refer [->Player]]
    [spellcast.logging :refer [log]]
    [spellcast.world :as world]
    ))

(def messages
  {"collision" "A player with that name is already in the game."
   "full" "The game is full."
   "toolate" "The game has already started."
   "ok" nil})

(defn- join-page [message]
  (let [message (get messages message "An unknown error has occurred.")]
    (html5
      [:head
       [:title "Spellcast"]
       (include-css "/css/spellcast.css")]
      [:body
       (form/form-to
         [:post "/join"]
         [:table#join
          [:tr.short [:th {:colspan 2} "Join Game"]]
          (when message
            [:tr.short [:th.error {:colspan 2} message]])
          [:tr [:td (form/label "name" "Name")]
               [:td (form/text-field "name")]]
          [:tr [:td (form/label "pronouns" "Pronouns")]
               [:td (form/drop-down "pronouns" ["they" "she" "he" "it"] "they")]]
          [:tr [:td {:colspan 2} (form/submit-button "Join Game")]]
          ])])))

(defn page [request]
  (if (-> (world/state) :phase (not= :pregame))
    (-> "A game is already in progress."
        r/response
        (r/status 409)
        (r/content-type "text/plain"))
    (join-page (get-in request [:params :message]))))

(defn- attempt-join [world params]
  (let [player (->Player params (-> world :settings :max-hp))]
    (cond
      (not= :pregame (world :phase))
      (throw+ "The game is already in progress.")
      (when (game/get-player world (player :name)))
      (throw+ "A player with that name is already in the game.")
      :else
      (-> world
          (game/add-player player)
          (log {:player (player :name)}
            :all "{{player}} has joined the game.")))))

(defn post
  ; On POST, attempt to join the player to the game.
  [request]
  (let [session (request :session)]
    (try+
      (world/update! attempt-join (request :params))
      (-> (r/redirect "/game" 303)
          (assoc :session
            (assoc session :name (-> request :params :name))))
      (catch string? err
        (error err)
        (-> err r/response (r/status 400))))))
