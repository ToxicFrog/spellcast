(ns spellcast.views.join
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
  (:require
    [hiccup.form :as form]
    [hiccup.page :refer [html5 include-css]]
    [ring.util.response :as r]
    ))

(defn- join-form []
  [:body
   (form/form-to [:post "/game/join"]
                 (form/label "name" "Name")
                 (form/text-field "name")
                 [:br]
                 (form/label "pronouns" "Pronouns")
                 (form/drop-down "pronouns" ["they" "she" "he" "it"] "they")
                 [:br]
                 (form/submit-button "Join Game"))])


(defn page [request]
  (html5
    [:head
     [:title "Spellcast"]
     (include-css "/css/screen.css")]
    (cond
      ; If they're already logged in just point them at the game in progress.
      (-> request :session :name string?) (r/redirect "/")
      ; If there's no room don't even show them the join form.
      ; false nil
      ; If they aren't logged in and the game isn't full, let them in.
      (-> request :session :name nil?) (join-form)
      :else (-> (r/response "Internal error in /game/join") (r/status 500)))))
