(ns spellcast.phase.common
  "Common event handlers and utilities."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
  (:require
    [ring.util.response :as r]
    [slingshot.slingshot :refer [throw+]]
    [spellcast.data.game :as game :refer [Game GamePhase]]
    [spellcast.logging :refer [log]]
    ))

(defschema Params {s/Keyword s/Str})
(defschema Response {s/Any s/Any})
(defschema EventResult
  (s/pair Game "world"
          (s/cond-pre s/Str Response) "response"))

(defn dispatcher :- [(s/one GamePhase "phase") (s/one s/Keyword "event-type")]
  ([_world phase event] [phase event])
  ([world _player request _body]
    [(world :phase) (-> request :params :evt keyword)]))

(defmulti dispatch-event dispatcher)

(defmethod dispatch-event :default :- Game
  ; Server tried to fire an event that there is no mapping for in the current
  ; phase.
  ([world _phase _event] world)
  ; Client sent a request that the current game phase doesn't understand.
  ([world _player request _body]
   (println "Bad request:" request)
   (-> (str "bad request: " (request :params) "\n"
         "world state is " world "\n"
         "full request is " request)
       (r/response)
       (r/status 400)
       (r/content-type "text/plain")
       (throw+))))

(defn post-log
  "Add a chat message from the given player."
  [world :- Game, player :- s/Str, body :- s/Str] :- EventResult
   (as-> world $
         (log $ {:name player :text body}
           player "You say, \"{{text}}\""
           :else "{{name}} says, \"{{text}}\"")
         $))
