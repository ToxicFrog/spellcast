(ns spellcast.game (:gen-class))
(require '[clojure.core.async :as async :refer [<! >! <!! >!! chan pub sub go close! thread]]
         '[spellcast.spells :refer [available-spells]]
         '[spellcast.util :refer :all]
         '[taoensso.timbre :as log])

(defmacro defstate [name & fns]
  (let [unknown (fn [game msg]
                  (log/warn "Unknown message type" msg)
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
    (prn (:name p) (available-spells (:left p) (:right p))))
  game)

(defn- run-turn [game]
  (prn "Starting turn" (:turn game))
  (-> game
      (update-in [:turn] inc)
      get-gestures
      ask-questions
      execute-turn
      ))

(defn- game-finished? [game]
  (< 0 (:turn game)))

(defn new-player [name]
  {:name name :left '() :right '()})

(defn- enough-players? [game]
  (> (count (:players game))
     (:min-players game)))

(defn- send-to [game user msg]
  (>!! (:out game) (into msg {:tag user}))
  game)

(defn- remove-player [game player]
  (update-in game [:players]
             dissoc
             (get-in game [:players player :id])
             (get-in game [:players player :name])))

(defn- disconnect [game user msg]
  (log/infof "Disconnecting user %d: %s" user msg)
  (send-to game user {:tag :error :msg msg})
  (send-to game user {:close true})
  (remove-player game user))

(defn- get-player [game key]
  (or ((:players game) key)
      (some #(= key (:name %)) (:players game))))

(defn- add-client [game {:keys [name pass from]}]
  ; reject if:
  ; another player already present with the same name
  ; game at player limit
  ; password required and wrong/no password provided
  (cond
    (get-player game from) (send-to game from {:msg "You are already logged in."})
    (get-player game name) (disconnect game from "A player with that name already exists.")
    (>= (count (:players game))
        (:max-players game)) (disconnect game from "Player limit reached.")
    (and (:password game)
         (not= pass (:pass game))) (disconnect game from "Password incorrect.")
    :else (let [player {:name name :id from}]
            (send-to game :all {:msg (str name " has joined the game.")})
            (-> game
                (assoc-in [:players from] player)))))

(defstate collect-players
  (def done? enough-players?)
  (defn begin [game]
    (log/info "Collecting players...")
    game)
  (defn end [game]
    (log/info "Got enough players!")
    game)
  (defn :login [game {:keys [from name] :as msg}]
    (if (string? name)
      (add-client game msg)
      (send-to game from {:msg "Malformed login request."})))
  (defn :disconnect [game msg]
    (remove-player game (:from msg))))

(defn run-state [game state]
  (log/debug "State runner init" game state)
  (let [in (:in game)
        {:keys [begin end done? handlers default]} state]
    (loop [game (begin game)]
      (log/debug "state iteration game=" game)
      (if (done? game)
        (end game)
        (let [msg (<!! in)
              handler (get handlers (:tag msg) default)]
          (log/debug "log msg=" msg)
          (recur (handler game msg)))))))

(defn- init-game
  "Perform game startup tasks like collecting players."
  [game]
  (log/debug "Initializing game...")
  (run-state game collect-players))

(defn- report-end [game]
  (prn "Finished:" game))

(defn run-game [game]
  (log/debug "Launching new gamerunner thread for" game)
  (try-thread "gamerunner"
    (loop [game (init-game game)]
      (log/info "run-game" (:turn game))
      (if (game-finished? game)
        (report-end game)
        (recur (run-turn game))))))

(defn new-game
  "Return a new Spellcast game state."
  [& {:as init}]
  (let [in (chan)
        out (chan)
        out-bus (pub out :tag)]
    (assoc init :in in
                :out out :out-bus out-bus
                :players {}
                :turn 0)))

