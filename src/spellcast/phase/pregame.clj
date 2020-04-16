(ns spellcast.phase.pregame
  "Event handlers for pregame phase. This is the phase just after server startup, while waiting for players to join, and ends once a quorum is reached. During this phase, all players can do is talk to each other."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
  (:require
    [ring.util.response :as r]
    [spellcast.data.game :as game]
    [spellcast.data.player :as player :refer [->Player]]
    [spellcast.logging :refer [log]]
    [spellcast.phase.common :as phase-common :refer [dispatch]]
    [spellcast.views.join :as views.join]
    ))

; Chat handlers common to all phases.
(defmethod dispatch [:pregame :log]
  ([world player _] (phase-common/get-log world player))
  ([world player _ body] (phase-common/post-log world player body)))


(defn- join-error [world player]
  (cond
    (= 2 (count (world :players))) "The game is full."
    (game/get-player world (player :name)) "A player with that name is already in the game."
    :else nil))

(defn- check-phase-exit [world]
  (if (= (-> world :players count) (-> world :settings :max-players))
    (assoc world :phase :ingame)
    world))

(defmethod dispatch [:pregame :BEGIN]
  ([world _]
   (println "Entering pregame phase...")
   world))

(defmethod dispatch [:pregame :join]
  ; On GET serve the "join game" page.
  ; We don't need to worry about the player already being logged in because
  ; the global redirect protects us from that.
  ([world _ request]
   (println "dispatch" world request)
   [world (views.join/page request)])
  ; On POST, attempt to join the player to the game.
  ([world _ request _]
   (let [player (->Player (request :params) (-> world :settings :max-hp))
         error (join-error world player)
         session (request :session)]
     (if error
       [world error]
       [(-> world
            (game/add-player player)
            (log {:player (player :name)}
              :all "{{player}} has joined the game.")
            check-phase-exit)
        (-> (r/redirect "/game" 303)
            (assoc :session session)
            (assoc-in [:session :name] (-> request :params :name)))]))))

(defmethod dispatch [:pregame :END]
  ([world _]
   (println "Leaving pregame phase...")
   world))
