(ns spellcast.game
  (:require
    [spellcast.state.player :refer [->Player PlayerParams]]
    [spellcast.state.game :as game :refer [Game ->Game LogFilter]]
    [schema.core :as s :refer [def defn defschema fn]]
            ))

(def SETTINGS {:max-players 2 :max-hp 15})

(def ^:private world
  (atom (->Game SETTINGS) :validator (s/validator Game)))

(defn state [] @world)
(defn reset! [] (swap! world (constantly (->Game SETTINGS))))

(defn add-player! [params :- PlayerParams]
  (swap! world game/add-player (->Player params (-> @world :settings :max-hp))))
  ; ([params :- PlayerParams] (swap! world add-player! params))
  ; ([world :- Game, {:keys [:name] :as params} :- PlayerParams]
  ;  (cond
  ;    (some? (get-in world [:players name]))
  ;    (throw (IllegalArgumentException. (str "A player named " name " is already in the arena.")))
  ;    (= (-> world :players count) (-> world :settings :max-players))
  ;    (throw (IllegalArgumentException. "The arena is full."))
  ;    :else (assoc-in world [:players name]
  ;            (params-to-player world params)))))

(defn get-log [name :- s/Str] :- [s/Str]
  (game/get-log @world name))

(defn log! [message :- s/Str, filter :- LogFilter]
  (swap! world game/add-log message filter))
