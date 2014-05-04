(ns spellcast.core (:gen-class))
(require '[clojure.core.async :as async :refer [<! >! <!! >!! chan pub sub go close!]]
         '[spellcast.spells :refer [available-spells]]
         '[spellcast.net :refer :all]
         '[taoensso.timbre :as log])

(defn record-gestures [player left right]
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

(defn- all [p xs]
  (not (some #(not (p %)) xs)))

(defn- game-finished? [game]
  (< 0 (:turn game)))

(defn new-player [name]
  {:name name :left '() :right '()})

(defn- enough-players? [game]
  (> (count (:players game))
     (:min-players game)))

(defn- send-to [game user msg]
  (>!! (:out game) (into msg {:tag user})))

(defn- remove-player [game player]
  (update-in game [:players]
             dissoc
             (get-in game [:players player :id])
             (get-in game [:players player :name])))

(defn- disconnect [game user msg]
  (send-to game user {:error msg})
  (send-to game user {:close true})
  (remove-player game user))

(defn- add-client [game {:keys [name pass from]}]
  ; reject if:
  ; another player already present with the same name
  ; game at player limit
  ; password required and wrong/no password provided
  (cond
    (-> game :players (contains? name)) (disconnect game from "A player with that name already exists.")
    (>= (count (:players game))
        (:max-players game)) (disconnect game from "Player limit reached.")
    (and (:password game)
         (not= pass (:pass game))) (disconnect game from "Password incorrect.")
    :else (let [player {:name name :id from}]
            (-> game
                (assoc-in [:players from] player)
                (assoc-in [:players name] player)))))

(defn- collect-players [{:keys [socket in-bus] :as game}]
  (log/info "collect-players init" game)
  (let [ch (sub in-bus :login (chan))]
    (loop [game game]
      (log/info "collect-players loop" game)
      (if (enough-players? game)
        (do
          (close! ch)
          game)
        (recur (add-client game (<!! ch)))))))

(defn- init-game
  "Perform game startup tasks like collecting players."
  [game]
  (let
    [in-ch (chan)
     out-ch (chan)
     in-bus (pub in-ch :tag)
     out-bus (pub out-ch :tag)
     socket (listen-socket 8666 in-ch out-bus)]
    (-> game
        (assoc
          :players {}
          :turn 0
          :in in-ch :in-bus in-bus
          :out out-ch :out-bus out-bus
          :socket socket)
        collect-players)))

(defn report-end [game]
  (prn "Finished:" game))

(defn- run-game [game]
  (loop [game (init-game game)]
    (log/info "run-game" (:turn game))
    (if (game-finished? game)
      (report-end game)
      (recur (run-turn game)))))

(def topic identity)

(defn- new-game
  "Return a new Spellcast game state."
  [& {:as init}]
  (let [ch (chan)
        pubsub (pub ch topic)]
    (into init {:ch ch
                :pubsub pubsub
                })))

(defn -main
  [& args]
  (log/infof "Spellcast starting up.")
  (run-game (new-game :min-players 2 :max-players 2 :allow-spectators false)))
