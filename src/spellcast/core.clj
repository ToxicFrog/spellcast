(ns spellcast.core (:gen-class))
(require '[clojure.core.async :as async :refer [<! >! <!! >!! chan pub sub go-loop]])
(require '[spellcast.spells :refer [available-spells]])
(require '[spellcast.net :refer :all])

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

(defn- init-game
  "Perform game startup tasks like collecting players."
  [game]
  (into game
        {:players (->> (range 0 (:min-players game)) (map #(str "player_" %)) (map new-player))
         :spectators []
         :turn 0
         }))

(defn report-end [game]
  (prn "Finished:" game))

(defn- run-game [game]
  (let [[ch sock] (listen-socket 8666)
        client-ch (chan)]
    (<!!
    (go-loop [client (<! ch)]
             (if client
               (do
                 (prn client)
                 (let [msg (<! (:from client))]
                   (prn msg)
                   (>! (:to client) msg)
                   (prn "message echoed"))
                 (close-client client)
                 (recur (<! ch)))
               (prn "listener died! eep!")))
    )
    (loop [game (init-game game)]
      (if (game-finished? game)
        (report-end game)
        (recur (run-turn game))))
    (.close sock)))

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
  (run-game (new-game :min-players 4 :max-players 4 :spectators false)))
