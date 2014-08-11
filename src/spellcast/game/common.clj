(ns spellcast.game.common)
(require '[taoensso.timbre :as log]
         '[clojure.core.async :refer [<!! >!!]]
         '[spellcast.util :refer :all])

(def ^:dynamic *game-out* nil)
(def ^:dynamic *game-in* nil)

(defmacro defphase
  "Define a phase of the game that can be executed with run-phase.

  The body supports both defs (e.g. (def begin identity)) and defns. Each fn's
  name is expected to be a keyword, which is the tag of the message type that
  fn will be called to handle. There are four exceptions with special meaning:
  begin, end, default, and done?.

  The default values for begin and end are identity; the default value for
  default logs a warning. done? has no default value and failure to provide it
  is an error.

  See the documentation for run-phase for details on game phase execution."
  [name & fns]
  (let [unknown (fn [game & rest]
                  (log/warn "Unknown message type" rest)
                  game)
        as-fn (fn [f]
                (cond
                  (= 'defn (first f)) [(second f) (cons 'fn (drop 2 f))]
                  (= 'def (first f)) [(second f) (nth f 2)]
                  :else (throw (Exception. "Bad clause in defstate; only def/defn permitted"))))
        fns (->> fns
                 (map as-fn)
                 (map (fn [f] [(first f) (second f)]))
                 (into {}))]
    `(def ~name
       {:begin ~(get fns 'begin identity)
        :end ~(get fns 'end identity)
        :default ~(get fns 'default unknown)
        :done? ~(get fns 'done?)
        :handlers ~(into {} (filter (fn [[k v]] (keyword? k)) fns))})))

(defn run-phase
  "Execute a game phase on a given gamestate. Returns a new gamestate
  representing the state of the game once the phase is finished.

  Note: begin, end, done?, default, and the handler functions mentioned here
  are not globals, but are defined with a (defphase).

  When execution begins, the game state is initialized with (begin game).

  It then loops, terminating when (done? game) returns true. Each turn, it
  reads a message from (:in game), splits the message into [tag & args], and
  then calls the handler with the same name as the tag, using (apply handler
  game args). If there is no such handler, it calls (apply default game tag args)
  instead.

  Once iteration is complete, it returns (end game)."
  [game state]
  (log/debug "State runner init" game state)
  (let [in (:in game)
        {:keys [begin end done? handlers default]} state]
    (loop [game (begin game)]
      (log/debug "state iteration game=" game)
      (log/debug "done=" (done? game))
      (if (done? game)
        (end game)
        (let [msg (<!! in)
              tag (keyword (first msg))
              handler (get handlers tag)
              args (cons (get-meta msg :id) (rest msg))]
          (log/debug "run-state handling message" (meta msg) msg)
          (if handler
            (recur (apply handler game args))
            (recur (apply default game tag args))))))))

(defn send-to [id msg]
  (log/debug "SEND-TO" id msg)
  (>!! *game-out* (with-meta msg {:id id})))

(defn send-except [id msg]
  (log/debug "SEND-XC" id msg)
  (>!! *game-out* (with-meta msg {:id :all :exclude #{id}})))

(defn every-player?
  "Returns true if p is true for all players in the game."
  [game p]
  (every? p (-> game :players vals)))

(defn chat-handler [game id msg]
  (send-to :all (list :chat id msg))
  game)

(defn get-player [game key]
  (or ((:players game) key)
      (->> game :players vals
           (filter #(= key (:name %)))
           first)))
