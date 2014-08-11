(ns spellcast.game.collect-players)
(require '[clojure.core.async :as async :refer [<! >! <!! >!! chan go pub close! thread]]
         '[spellcast.util :refer :all]
         '[spellcast.game.common :refer :all]
         '[taoensso.timbre :as log])

(defn- new-player [name id]
  {:name name :id id :left '() :right '()})

(defn- remove-player [game player]
  (update-in game [:players]
             dissoc
             (get-in game [:players player :id])))

(defn- disconnect [game user msg]
  (log/infof "Disconnecting user %d: %s" user msg)
  (send-to user (list :error msg))
  (send-to user '(:close))
  (remove-player game user))

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
            (-> game
                (assoc-in [:players id] player)))))

(defphase collect-players
  (defn done? [game]
    (and
      (>= (count (:players game)) (:min-players game))
      (every-player? game :ready)))
  (defn begin [game]
    (log/info "Collecting players...")
    game)
  (defn end [game]
    (log/info "Got enough players!")
    game)
  (def :chat chat-handler)
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
