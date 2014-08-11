(ns spellcast.game (:gen-class))
(require '[clojure.core.async :as async :refer [<! >! <!! >!! chan go pub close! thread]]
         '[clojure.algo.generic.functor :refer [fmap]]
         '[spellcast.spells :refer [available-spells]]
         '[spellcast.util :refer :all]
         '[spellcast.game.common :refer :all]
         '[spellcast.game.collect-players :refer :all]
         '[taoensso.timbre :as log])

(defn- update-players [game f & args]
  (assoc game :players
    (fmap #(apply f % args) (:players game))))

(defn- record-gestures [player left right]
  (merge-with conj player {:left left :right right}))

(defn- get-gestures [game]
  ; wait for gesture messages from all clients, then drop the listener
  ; and insert them into the game
  (update-players game record-gestures :f :p))

(defn- ask-questions [game]
  game)

(defn- execute-turn [game]
  (log/debug "execute-turn" game)
  (doseq [[id player] (game :players)]
    (log/debug (:name player) (available-spells (:left player) (:right player))))
  game)

(defn- unready-all [game]
  (update-players game assoc :ready false))

(defn- run-turn [game]
  (log/info "Starting turn" (:turn game))
  (-> game
      (update-in [:turn] inc)
      unready-all
      get-gestures
      execute-turn
      ))

(defn- game-finished? [game]
  (< 2 (:turn game)))

(defn- init-game
  "Perform game startup tasks like collecting players."
  [game]
  (log/debug "Initializing game...")
  (run-phase game collect-players))

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
      (log/debug "Starting game iteration.")
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
