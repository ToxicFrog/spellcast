(ns spellcast.phase.pregame
  "Event handlers for pregame phase. This is the phase just after server startup, while waiting for players to join, and ends once a quorum is reached. During this phase, all players can do is talk to each other."
  (:require
    [clojure.string :as string]
    [spellcast.logging :refer [log]]
    [spellcast.state.event :refer [dispatch]]
    [spellcast.state.game :as game :refer [Game GameState]]
    [spellcast.state.player :as player :refer [->Player]]
    [ring.util.response :as r]
    [spellcast.views.join :as views.join]
    [schema.core :as s :refer [def defn defschema defmethod fn]]))

; This should be pulled into a common function that is used in all phases, since
; chatting is allowed at all times.
(defmethod dispatch [:pregame :log]
  ([world player params]
   (as-> world $
         (game/get-log $ player)
         (string/join "<br>\n" $)
         (r/response $)
         (r/content-type $ "text/html")
         [world $]))
  ([world player params body]
   (as-> world $
         (log $ {:name player :text body}
           player "You say, \"{{text}}\""
           :else "{{name}} says, \"{{text}}\"")
         [$ (r/response nil)])))

(defn join-error [world player]
  (cond
    (= 2 (count (world :players))) "The game is full."
    (game/get-player world (player :name)) "A player with that name is already in the game."
    :else nil))


(defmethod dispatch [:pregame :join]
  ; On GET serve the "join game" page.
  ; We don't need to worry about the player already being logged in because
  ; the global redirect protects us from that.
  ([world _ request]
   (println "dispatch" world request)
   [world (views.join/page request)])
  ; On POST, attempt to join the player to the game.
  ([world _ request body]
   (let [player (->Player (request :params) (-> world :settings :max-hp))
         error (join-error world player)
         session (request :session)]
     (if error
       [world error]
       [(-> world
            (game/add-player player)
            (log {:player (player :name)}
              :all "{{player}} has joined the game."))
        (-> (r/redirect "/game" 303)
            (assoc :session session)
            (assoc-in [:session :name] (-> request :params :name)))]))))
