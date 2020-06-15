(ns spellcast.views.join
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
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

(defn- join-form []
  [:body
   (form/form-to [:post "/join"]
                 (form/label "name" "Name")
                 (form/text-field "name")
                 [:br]
                 (form/label "pronouns" "Pronouns")
                 (form/drop-down "pronouns" ["they" "she" "he" "it"] "they")
                 [:br]
                 (form/submit-button "Join Game"))])

(defn- join-page []
  (html5
    [:head
     [:title "Spellcast"]
     (include-css "/css/screen.css")]
    (join-form)))

(defn get [_]
  (if (-> (world/state) :phase (not= :pregame))
    (-> "A game is already in progress." r/response (r/status 409))
    (join-page)))

(defn- check-game-start [world]
  (if (= (-> world :settings :max-players) (-> world :players count))
    ; TODO run phase-entry code
    (assoc world :phase :ingame)
    world))

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
           :all "{{player}} has joined the game.")
         check-game-start))))

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
       (println err)
       (-> err r/response (r/status 400))))))
