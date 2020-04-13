(ns spellcast.phase.common
  "Common event handlers and utilities."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
  (:require
    [clojure.string :as string]
    [ring.util.response :as r]
    [spellcast.logging :refer [log]]
    [spellcast.state.event :refer [EventResult]]
    [spellcast.state.game :as game :refer [Game]]
    [spellcast.state.player :refer [Player]]
    ))

(defn get-log
  "Return the game log as viewed by the given player."
  [world :- Game, player :- s/Str] :- EventResult
  (as-> world $
        (game/get-log $ player)
        (string/join "<br>\n" $)
        (r/response $)
        (r/content-type $ "text/html")
        [world $]))

(defn post-log
  "Add a chat message from the given player."
  [world :- Game, player :- s/Str, body :- s/Str] :- EventResult
   (as-> world $
         (log $ {:name player :text body}
           player "You say, \"{{text}}\""
           :else "{{name}} says, \"{{text}}\"")
         [$ (r/response nil)]))
