(ns spellcast.world
  "The atom holding the game state, and functions to manipulate it."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [ring.util.response :as r]
    [slingshot.slingshot :refer [try+]]
    [spellcast.data.game :as game :refer [Game ->Game]]
    [spellcast.logging :as logging]
    [spellcast.phase.common :as phase]
    ))

(def SETTINGS {:max-players 2 :max-hp 15})

(def ^:private world
  (atom (->Game SETTINGS) :validator (s/validator Game)))

(defn state [] @world)
(defn reset-game! [] (reset! world (->Game SETTINGS)))

(defn get-log :- [s/Str]
  [name :- s/Str]
  (game/get-log @world name))

(defn log! :- Game
  "Log one or messages. Same arguments as logging/log.
  Non-transactional (other things may happen in between log messages) but messages are guaranteed to be logged in the order they appear here. Log messages from multiple simultaneous calls to log! may be interleaved, but all players are guaranteed to see the messages in the same order."
  [bindings :- {s/Keyword s/Any}, & rest]
  (apply swap! world logging/log bindings rest))

(defn- updater
  [world f rest]
  (debug "updater" f (world :phase))
  (let [world' (apply f world rest)
        next (phase/dispatch-event world' (world' :phase) :NEXT)]
    (when next
      (info "New phase:" (world :phase) "->" next))
    (if (not next)
      world'
      (-> world'
          (phase/dispatch-event (world' :phase) :END)
          (assoc :phase next)
          (phase/dispatch-event next :BEGIN)))))

(defn update!
  "Update the game state. Same signature as swap! except that the atom parameter is implicit.
  If the swapping function returns a game state with a different phase than the original, also calls the phase exit and phase entry functions for the current and previous phases."
  [f & rest] (swap! world updater f rest))

(defn POST! :- (s/pred r/response?)
  "Respond to a POST request from a player. The request handler must either:
  - return a new game state, which is swapped in; the client gets 200
  - throw a Response, which is returned to the client unaltered
  - throw a String, which is wrapped in HTTP 500 and returned to the client
  - throw anything else, which will produce an error page via the Ring error handler
  In the latter three cases the world state is left unaltered."
  [player request body]
  (try+
    (update! phase/dispatch-event player request body)
    (r/response "")
    (catch r/response? resp resp)
    ; (catch string? s (-> (r/response s) (r/status 500)))
    ))

(defn watch
  "Watch the game state for changes. Given a function that extracts the interesting part of the game state and the hash of the old version, blocks until (-> @world key-fn hash) is different from the old hash value, then returns (key-fn @world)."
  [key-fn old-hash]
  (let [key (promise)
        watcher (fn watcher [key _atom _val val']
                  (when (not= old-hash (hash (key-fn val')))
                    (deliver key val')
                    (remove-watch world key)))]
    (add-watch world key watcher)
    ; Avoid race condition here -- it's possible that between watch being called
    ; and the watch being added, the world got updated by another thread. So we
    ; check here to see if the world got updated while we were setting the watch,
    ; in which case we discard both the watch and the promise.
    (if (not= old-hash (hash (key-fn @world)))
      (do
        (remove-watch world key)
        (key-fn @world))
      (key-fn @key))))
