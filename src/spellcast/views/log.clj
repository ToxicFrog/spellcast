(ns spellcast.views.log
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
  (:require
    [hiccup.core :refer [html]]
    [ring.util.response :as r]
    [spellcast.data.game :as game]
    [spellcast.world :as world]
    ))

(defn- to-html [log]
  (html
    [:div
     [:input#log-index {:type :hidden :value (hash log)}]
     (interpose [:br] log)]))

(defn page [player index]
  (as-> (world/watch #(game/get-log % player) index) $
        (to-html $)
        (r/response $)
        (r/content-type $ "text/html")
        $))
