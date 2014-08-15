(ns spellcast.game.collect-players)
(require '[clojure.core.async :as async :refer [<! >! <!! >!! chan go pub close! thread]]
         '[spellcast.util :refer :all]
         '[spellcast.game.common :refer :all]
         '[taoensso.timbre :as log])

(defn- new-player [name id]
  {:name name :id id :left '() :right '()})

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
    :else (let [player (new-player name id)]
            (log/info "Player" id "logged in as" name)
            (send-to :all (list :info (str name " has joined the game.")))
            (assoc-in game [:players id] player))))

(defphase collect-players phase-defaults
  (defn done? [game]
    (and
      (>= (count (:players game)) (:min-players game))
      (every-player? game :ready)))
  (defn get-handler [phase game tag & args]
    (if (= :login tag)
      (phase :login)
      (apply (phase-defaults 'get-handler) phase game tag args)))
  (defn :login [game id name]
    (if (string? name)
      (add-client game id name)
      (do (send-to id (list :error "Malformed login request.")) game))))
