(ns spellcast.phase.pregame
  "Event handlers for pregame phase. This is the phase just after server startup, while waiting for players to join, and ends once a quorum is reached. During this phase, all players can do is talk to each other."
  (:require
    [clojure.string :as string]
    [spellcast.logging :refer [log]]
    [spellcast.state.event :refer [dispatch]]
    [spellcast.state.game :as game :refer [Game GameState]]
    [ring.util.response :as r]
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
