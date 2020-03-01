(ns spellcast.views.join
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.form :as form]
            [ring.util.response :as r]
            [spellcast.game :as game]
            [spellcast.logging :refer [log]]
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


(defn post [request]
  (let [session (request :session)
        params (request :params)]
    (game/add-player! params)
    (log {:player (params :name)}
      :all "{{player}} has joined the game.")
    (-> (r/redirect "/game" 303)
        (assoc :session session)
        (assoc-in [:session :name] (params :name)))))

(defn page [request]
  (html5
    [:head
     [:title "Spellcast"]
     (include-css "/css/screen.css")]
    (cond
      ; If they're already logged in just point them at the game in progress.
      (-> request :session :name string?) (r/redirect "/")
      ; If there's no room don't even show them the join form.
      false nil
      ; If they aren't logged in and the game isn't full, let them in.
      (-> request :session :name nil?) (join-form)
      :default (-> (r/response "Internal error in /join") (r/status 500)))))
