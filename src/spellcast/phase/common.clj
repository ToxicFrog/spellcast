(ns spellcast.phase.common
  "Common event handlers and utilities."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
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
  ([_world phase event]
   (debug "EVENT" phase event)
   [phase event])
  ([world _player request _body]
   (debug "POST " (world :phase) (request :uri))
    [(world :phase) (-> request :params :evt keyword)]))

(defmulti dispatch-event dispatcher)

(defmethod dispatch-event :default ; :- Game
  ; Server tried to fire an event that there is no mapping for in the current
  ; phase.
  ([world _phase _event] world)
  ; Client sent a request that the current game phase doesn't understand.
  ([world _player request _body]
   (warn "Bad request:" request)
   (-> (str "bad request: " (request :params) "\n"
         "world state is " world "\n"
         "full request is " request)
       (r/response)
       (r/status 400)
       (r/content-type "text/plain")
       (throw+))))

(defn phase-info [world]
  (dispatch-event world (world :phase) :INFO))

(defn post-log
  "Add a chat message from the given player."
  [world :- Game, player :- s/Str, {:keys [text]} :- {:text s/Str}] :- EventResult
   (as-> world $
         (log $ {:name player :text text}
           player "You say, \"{{text}}\""
           :else "{{name}} says, \"{{text}}\"")
         $))
