(ns spellcast.game
  (:require [schema.core :as s :refer [def defn defschema fn]]
            ))

(defschema Player
  {:name s/Str
   :pronouns (s/enum :they :she :he :it)
   ; FIXME we can't depend on spellcast.logging because it's a cycle
   ; (apply s/enum (keys logging/pronouns-map))
   :hp s/Int
   ; :gestures s/Any ; gesture history
   ; :status s/Any ; status effects -- map from effect to duration?
   })

(defschema LogFilter
  (s/cond-pre
    #{s/Str}
    (s/=> (s/maybe s/Str) s/Bool)))

(defschema LogMessage
  "A log message consists of a log line followed by a set of players the log message is meant to be visible to, identified by name.
  A log can also be marked as visible to :all, in which case all players can see it."
  [(s/one s/Str "message") (s/one LogFilter "vis")])

(defschema Game
  {:players {s/Str Player}
   :settings {:max-players s/Int
              :max-hp s/Int}
   :log [LogMessage]})

(def ^:private world
  (atom {:players {} :log []
         :settings {:max-players 2 :max-hp 15}}
    :validator (s/validator Game)))

(defschema PlayerParams
  {:name s/Str :pronouns s/Str})

(defn ^:private params-to-player [world :- Game, {:keys [name pronouns]}]
  {:name name
   :pronouns (keyword pronouns)
   :hp (-> world :settings :max-hp)})

(defn add-player!
  ([params :- PlayerParams] (swap! world add-player! params))
  ([world :- Game, {:keys [:name] :as params} :- PlayerParams]
   (cond
     (some? (get-in world [:players name]))
     (throw (IllegalArgumentException. (str "A player named " name " is already in the arena.")))
     (= (-> world :players count) (-> world :settings :max-players))
     (throw (IllegalArgumentException. "The arena is full."))
     :else (assoc-in world [:players name]
             (params-to-player world params)))))

(defn player [name :- s/Str] :- Player
  (-> (get-in @world [:players name])
      (assert (str "Attempt to fetch missing player " name))))

(defn reset! []
  (swap! world
         (constantly {:players {} :log []
          :settings {:max-players 2 :max-hp 15}})))

(defn state [] @world)

(defn log! [message :- s/Str, filter :- LogFilter]
  (swap! world update :log conj [message filter]))

(defn get-log [name]
  (->> (@world :log)
       (filter (fn [[_ vis]] (vis name)))
       (map first)))
