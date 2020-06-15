(ns spellcast.views.log
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
  (:require
    [clojure.string :as string]
    [ring.util.response :as r]
    [spellcast.world :as game]
    ))

(defn page [_request player]
  (as-> (game/get-log player) $
        (string/join "<br>\n" $)
        (r/response $)
        (r/content-type $ "text/html")
        $))
