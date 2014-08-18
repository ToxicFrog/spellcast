(ns spellcast.game (:gen-class))
(require '[clojure.core.async :as async :refer [<! >! <!! >!! chan go pub close! thread]]
         '[spellcast.spells :refer [available-spells]]
         '[spellcast.util :refer :all]
         '[spellcast.game.common :refer :all]
         '[spellcast.game.collect-players :refer :all]
         '[spellcast.game.collect-gestures :refer :all]
         '[taoensso.timbre :as log])

(defn- ask-questions [game]
  game)

(defn- execute-turn [game]
  (log/trace "execute-turn" game)
  (doseq [[id player] (game :players)]
    (log/debug (:name player) (available-spells (:left player) (:right player))))
  game)

(defn- reveal-gestures [game]
  (send-player-info game [:left :right]))

(defn- run-turn [game]
  (log/info "Starting turn" (:turn game))
  (send-to :all (list :turn (:turn game)))
  (-> game
      (run-phase collect-gestures)
      ;(run-phase pre-reveal)
      (reveal-gestures)
      ;(run-phase post-reveal)
      execute-turn
      (update-in [:turn] inc)
      ))

(defn- game-finished? [game]
  (< 3 (:turn game)))

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
      (log/trace "Starting game iteration.")
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
                :turn 1)))
