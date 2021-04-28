(ns spellcast.views.gestures
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [hiccup.core :refer [html]]
    [ring.util.response :as r]
    [spellcast.data.game :as game]
    [spellcast.world :as world]
    ))

(defn- to-html [gestures]
  (html
    [:div
     [:input#log-index {:type :hidden :value (hash gestures)}]
     (interpose [:br] gestures)]))

(defn page [player index] {})

; (defn page
;   "Return "
;   [player index]
;   (as-> (world/watch #(game/get-gestures % player) index) $
;         (to-html $)
;         (r/response $)
;         (r/content-type $ "text/html")
;         $))
