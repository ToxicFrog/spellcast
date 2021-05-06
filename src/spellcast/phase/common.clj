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
  ([_world phase event]
   (throw+ (str "No handler for internal event " event " defined in phase " phase)))
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

(defn post-log :- Game
  "Add a chat message from the given player."
  [world :- Game, player :- s/Str, {:keys [text]} :- {:text s/Str}]
   (as-> world $
         (log $ {:name player :text text}
           player "You say, \"{{text}}\""
           :else "{{name}} says, \"{{text}}\"")
         $))

(defn- expand-reply [phase [_reply name [world] & body]]
  `(defmethod dispatch-event [~phase ~(keyword name)]
     [~world _phase# _event#]
     ~@body))

(defn- expand-event [phase [_event name [world player event] & body]]
  `(defmethod dispatch-event [~phase ~(keyword name)]
     [~world ~player _request# ~event]
     ~@body))

(defn- expand-reply-or-event [phase ast]
  (condp = (first ast)
    'reply (expand-reply phase ast)
    'event (expand-event phase ast)
    ast))

(defmacro defphase
  "Declare a game phase and a body of handlers for it. Handlers have the form:
    (reply MESSAGE [world] ...)
  Used for internal messages like :BEGIN, :END, and :INFO and should return the appropriate response for the message type.
    (event event-name [world player event] ...)
  Used for events from the client like :gesture and :ready. Should return the new world state based on handling the event. player is the name of the player sending the event, and event is the actual payload -- the structure will depend on what event type it is.
  Other top-level forms like def and defn can be included and will be passed through unmodified."
  [name & body]
  (let [phase (keyword name)]
  `(do
     (defmethod dispatch-event [~phase :log]
       [world# player# _request# body#] (post-log world# player# body#))
     ~@(map (partial expand-reply-or-event phase) body))))
