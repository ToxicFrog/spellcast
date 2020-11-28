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
    [spellcast.logging :refer [log]]
    [spellcast.world :as world]
    ))

(def messages
  {"collision" "A player with that name is already in the game."
   "full" "The game is full."
   "toolate" "The game has already started."
   "noname" "Please enter a name."
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
    (join-page "toolate")
    (join-page (get-in request [:params :message] "ok"))))

(defn- attempt-join [world {:keys [name pronouns] :as params}]
    (cond
      (not= :pregame (world :phase)) (throw+ "toolate")
      (= (count (world :players)) (-> world :settings :max-players)) (throw+ "full")
      (empty? name) (throw+ "noname")
      (game/get-player world name) (throw+ "collision")
      :else (-> world
                (game/add-player name (keyword pronouns))
                (log {:player name}
                  :all "--- {{player}} has joined the game."))))

(defn post
  ; On POST, attempt to join the player to the game.
  ; We handle this here rather than through the normal world/POST! mechanism
  ; because we need to construct a special response -- this is a "user-facing"
  ; request rather than an XHR and we need to redirect to the right place and
  ; provide meaningful error messages even when it's called at the wrong time.
  [request]
  (let [session (request :session)]
    (try+
      (world/update! attempt-join (request :params))
      (-> (r/redirect "/game" 303)
          (assoc :session session)
          (assoc-in [:session :name] (-> request :params :name)))
      (catch string? err
        ; If it's an error we have explicit handling for, attempt-join will throw an error code string,
        ; and we bounce the player back to /join with a query-string parameter telling it which
        ; error code to display.
        (r/redirect (str "/join?" err) 303)))))
