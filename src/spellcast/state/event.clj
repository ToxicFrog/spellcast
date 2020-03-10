(ns spellcast.state.event
  "Dispatcher for event handling."
  (:require
    [spellcast.state.game :as game :refer [Game GameState]]
    [schema.core :as s :refer [def defn defschema defmethod fn]]))

(defschema Params {s/Keyword s/Str})
(defschema Response {s/Any s/Any})
(defschema EventResult (s/pair Response "world" Response "response"))

(defmulti dispatch
  (fn dispatcher ; :- (s/pair GameState "state" s/Keyword "event")
;    [world :- Game, player :- s/Str, params :- (s/optional Params "params"), & rest]
    [world, player, params, & rest]
    [(world :state) (-> params :evt keyword)]))

(defmethod dispatch :default :- EventResult
  ([world player params]
   [world {:status 200 :body (str "bad request: " params)}])
  ([world player params body]
   [world {:status 200 :body (str "bad request: " params " // " body)}]))

; We can do multiple dispatch on [gamestate action] pairs
; So, each gamestate does something like:
; defmethod state/event [:pregame :join] [world event args...] ...
; defmethod state/event [:pregame :talk] [world event args...] ...
; and then at the top level we have a
; defmethod event :default [world event & rest] world
; which doesn't update the world at all and returns some sort of error
; This lets each gamestate declare handlers just for the events it
; cares about and be confident that the rest will be properly rejected.
; then the router maps a bunch of events to
; we can also do methods on arity, e.g.
; (defmethod event [:pregame :chat] ([world] ...return log) ([world request] ...write to log))
; so as to handle both GET and PUT in the same place.
; state/event world

; Things we always want passed: the current world state, the name of the player,
; the path of the request
; Things we get on POST: the request parameters
; What if we want parameters on GET? I'd rather dispatch entirely on arity, but maybe
; it would be better to do:)
; (defmethod [:pregame "log" :get] [world player params] ...)
; (defmethod [:pregame "log" :post] [world player params] ...)
; rather than
; (defmethod [:pregame "log"]
;    ([world player] ...get)
;    ([world player params] ...post))
; It would also be good to have some sort of fallthrough for events that are the
; same in every phase, but maybe log is the only one of them?

; (defmulti tick-world :state)

; (defn- game-full? [world]
;   (let [nplayers (count (world :players))
;         maxplayers (-> world :settings :max-players)]
;     (< nplayers maxplayers)))

; (defn- pregame-to-ingame [world]
;   )

; (defmethod tick-world :pregame [world :- Game] :- Game
;   (cond
;     (game-full? world) world
;     :else world))
;     (< (-> world :players count) (-> world :settings :max-players)) world

;     ))
