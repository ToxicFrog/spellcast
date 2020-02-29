(ns spellcast.game
  (:require [schema.core :as s :refer [def defn defschema fn]]
            [spellcast.text :as text]))

(defschema Player
  {:name s/Str
   :pronouns (apply s/enum (keys text/pronouns-map))
   :log [s/Str]
   :hp s/Int
   ; :gestures s/Any ; gesture history
   ; :status s/Any ; status effects -- map from effect to duration?
   })

(defschema Game
  {:players {s/Str Player}
   :settings {:max-players s/Int
              :max-hp s/Int}
   ; Third-person spectator log.
   :log [s/Str]})

(def ^:private world
  (atom {:players {} :log []
         :settings {:max-players 2 :max-hp 15}}
    :validator (s/validator Game)))

(defschema PlayerParams
  {:name s/Str :pronouns s/Str})
(defn ^:private params-to-player [world :- Game, {:keys [name pronouns]}]
  {:name name
   :pronouns (keyword pronouns)
   :log []
   :hp (-> world :settings :max-hp)})
(defn add-player!
  ([params :- PlayerParams] (swap! world add-player params))
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
