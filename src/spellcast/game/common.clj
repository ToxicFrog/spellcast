(ns spellcast.game.common)
(require '[taoensso.timbre :as log]
         '[clojure.algo.generic.functor :refer [fmap]]
         '[clojure.core.async :refer [<!! >!!]]
         '[spellcast.util :refer :all])

(def ^:dynamic *game-out* nil)
(def ^:dynamic *game-in* nil)

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

(defn get-player [game key]
  (or ((:players game) key)
      (->> game :players vals
           (filter #(= key (:name %)))
           first)))

(defn update-players [game f & args]
  (assoc game :players
    (fmap #(apply f % args) (:players game))))

(defn unready-all [game]
  (update-players game assoc :ready false))

(defn remove-player [game player]
  (update-in game [:players]
             dissoc
             (get-in game [:players player :id])))

(defn disconnect [game user msg]
  (log/infof "Disconnecting user %d: %s" user msg)
  (send-to user (list :error msg))
  (send-to user '(:close))
  (remove-player game user))

(defmacro defphase
  "Define a phase of the game that can be executed with run-phase.

  The body supports both defs (e.g. (def begin identity)) and defns. Each fn's
  name is expected to be a keyword, which is the tag of the message type that
  fn will be called to handle. There are four exceptions with special meaning:
  begin, end, get-handler, and done?. See the documentation for run-phase for
  details on when and how these are invoked.

  See the documentation for run-phase for details on game phase execution."
  [name defaults & fns]
  (let [as-fn (fn [f]
                (cond
                  (= 'defn (first f)) [(second f) (cons 'fn (drop 2 f))]
                  (= 'def (first f)) [(second f) (nth f 2)]
                  :else (throw (Exception. "Bad clause in defstate; only def/defn permitted"))))
        fns (->> fns
                 (map as-fn)
                 (map (fn [f]
                        (if (keyword? (first f))
                          [(first f) (second f)]
                          [`(quote ~(first f)) (second f)])))
                 vec)
        ]
    `(def ~name (into ~defaults ~fns))))

(defphase phase-defaults {}
  (def begin identity)
  (def end identity)
  (defn get-handler [phase game tag id & args]
    (if (get-player game id)
      ; Message from logged-in player
      (if-let [handler (phase tag)]
        handler
        ; But this phase doesn't know how to handle it.
        (fn [game & _]
          (log/info "Unknown message" tag "from player" id)
          game))
      ; Player is not logged in.
      (do
        (log/info "Rejecting message" tag "from non-logged-in player" id)
        (fn [game id & _]
          (disconnect game id "You must :login first.")))))
  (defn :disconnect [game id]
    (remove-player game id))
  (defn :chat [game id msg]
    (send-to :all (list :chat id msg))
    game)
  (defn :ready [game id ready]
    (send-to :all (list :player id :ready ready))
    (assoc-in game [:players id :ready] ready)))

(defn run-phase
  "Execute a game phase on a given gamestate. Returns a new gamestate
  representing the state of the game once the phase is finished.

  The phase should be defined with (defphase).

  When execution begins, the game state is initialized with (begin game).

  It then loops, terminating when (done? game) returns true. Each turn, it
  reads a message from (:in game) and splits it into [tag & args]. It then calls
  (get-handler phase game tag args...) to get a handler for the message, and
  then calls the handler as (handler game args...). The handler is expected to
  return the new game state.

  Once iteration is complete, it returns (end game)."
  [game phase]
  (let [in (:in game)
        {:syms [begin end done? get-handler]} phase]
    (log/debug "run-phase" begin end done? get-handler phase)
    (loop [game (begin game)]
      (if (done? game)
        (end game)
        (let [msg (<!! in)
              tag (keyword (first msg))
              args (cons (get-meta msg :id) (rest msg))
              handler (apply get-handler phase game tag args)]
          (log/debug "Handling messge" msg)
          (recur (apply handler game args)))))))
