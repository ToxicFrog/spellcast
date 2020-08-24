(ns spellcast.views.log
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

(defn page [player index]
  (as-> (world/watch #(game/get-log % player) index) $
        {:log $ :stamp (hash $)}
        (r/response $)
        (r/content-type $ "text/html")
        $))
