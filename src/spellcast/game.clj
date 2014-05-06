(ns spellcast.game (:gen-class))
(require '[clojure.core.async :as async :refer [<! >! <!! >!! chan go pub close! thread]]
         '[spellcast.spells :refer [available-spells]]
         '[spellcast.util :refer :all]
         '[taoensso.timbre :as log])

(def ^:dynamic *game-out* nil)
(def ^:dynamic *game-in* nil)

(defmacro defstate [name & fns]
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

(defn- record-gestures [player left right]
  (merge-with conj player {:left left :right right}))

(defn- get-gestures [game]
  ; wait for gesture messages from all clients, then drop the listener
  ; and insert them into the game
  (assoc game :players
    (->> (:players game)
         (map #(record-gestures % :f :f)))))

(defn- ask-questions [game]
  game)

(defn- execute-turn [game]
  (doseq [p (game :players)]
    (log/debug (:name p) (available-spells (:left p) (:right p))))
  game)

(defn- run-turn [game]
  (log/info "Starting turn" (:turn game))
  (-> game
      (update-in [:turn] inc)
      get-gestures
      execute-turn
      ))

(defn- game-finished? [game]
  true)

(defn new-player [name]
  {:name name :left '() :right '()})

(defn- send-to [id msg]
  (log/debug "SEND-TO" id msg)
  (>!! *game-out* (with-meta msg {:id id})))

(defn- send-except [id msg]
  (log/debug "SEND-XC" id msg)
  (>!! *game-out* (with-meta msg {:id :all :exclude #{id}})))

(defn- remove-player [game player]
  (update-in game [:players]
             dissoc
             (get-in game [:players player :id])
             (get-in game [:players player :name])))

(defn- disconnect [game user msg]
  (log/infof "Disconnecting user %d: %s" user msg)
  (send-to user (list :error msg))
  (send-to user '(:close))
  (remove-player game user))

(defn- get-player [game key]
  (or ((:players game) key)
      (some #(= key (:name %)) (-> game :players vals))))

(defn- add-client [game id name]
  ; reject if:
  ; another player already present with the same name
  ; game at player limit
  ; password required and wrong/no password provided
  (cond
    (get-player game id) (do (send-to id (list :error "You are already logged in.")) game)
    (get-player game name) (disconnect game id "A player with that name already exists.")
    (>= (count (:players game))
        (:max-players game)) (disconnect game id "Player limit reached.")
    (and (:password game)
         (not= nil (:pass game))) (disconnect game id "Password incorrect.")
    :else (let [player {:name name :id id}]
            (log/info "Player" id "logged in as" name)
            (send-to :all (list :info (str name " has joined the game.")))
            (-> game
                (assoc-in [:players id] player)))))

(defn- chat [game id msg]
  (send-to :all (list :chat id msg))
  game)

(defstate collect-players
  (defn done? [game]
    (and
      (>= (count (:players game)) (:min-players game))
      (every? :ready (vals (:players game)))))
  (defn begin [game]
    (log/info "Collecting players...")
    game)
  (defn end [game]
    (log/info "Got enough players!")
    game)
  (def :chat chat)
  (defn :login [game id name]
    (if (string? name)
      (add-client game id name)
      (do (send-to id (list :error "Malformed login request.")) game)))
  (defn :ready [game id ready]
    (if (get-player game id)
      (do (send-to :all (list :info (str id " ready: " ready)))
        (assoc-in game [:players id :ready] ready))
      (do (send-to id (list :error "You are not logged in.")) game)))
  (defn :disconnect [game id]
    (remove-player game id)))

(defn run-state [game state]
  (log/debug "State runner init" game state)
  (let [in (:in game)
        {:keys [begin end done? handlers default]} state]
    (loop [game (begin game)]
      (log/debug "state iteration game=" game)
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

(defn- init-game
  "Perform game startup tasks like collecting players."
  [game]
  (log/debug "Initializing game...")
  (run-state game collect-players))

(defn- report-end [game]
  (log/info "Game over!")
  (log/debug "Final game state:" game)
  (send-to :all (list :info "Finished!"))
  (send-to :all '(:close))
  (close! (:in game))
  game)

(defn run-game [game]
  (binding [*game-out* (:out game)
            *game-in* (:in game)]
    (loop [game (init-game game)]
      (if (game-finished? game)
        (report-end game)
        (recur (run-turn game))))))

(defn new-game
  "Return a new Spellcast game state."
  [& {:as init}]
  (let [in (chan)
        out (chan)
        out-bus (pub out #(get-meta % :id))]
    (assoc init :in in
                :out out :out-bus out-bus
                :players {}
                :turn 0)))

