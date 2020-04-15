(ns spellcast.phase.common
  "Common event handlers and utilities."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [clojure.pprint :refer [pprint]])
  (:require
    [clojure.string :as string]
    [ring.util.response :as r]
    [spellcast.data.game :as game :refer [Game]]
    [spellcast.logging :refer [log]]
    ))

(defschema Params {s/Keyword s/Str})
(defschema Response {s/Any s/Any})
(defschema EventResult
  (s/pair Game "world"
          (s/cond-pre s/Str Response) "response"))

(defmulti dispatch
  (fn dispatcher
    [world _player request & _body]
    (println "DISPATCH" [(world :phase) (-> request :params :evt keyword)])
    [(world :phase) (-> request :params :evt keyword)]))

(defmethod dispatch :default :- EventResult
  ([world _player request]
   [world (-> (str "bad request: " (request :params) "\n"
                   "world state is " world "\n"
                   "full request is " request)
              (r/response)
              (r/content-type "text/plain"))])
  ([world player request _body] (dispatch world player request)))

(defn get-log
  "Return the game log as viewed by the given player."
  [world :- Game, player :- s/Str] :- EventResult
  (as-> world $
        (game/get-log $ player)
        (string/join "<br>\n" $)
        (r/response $)
        (r/content-type $ "text/html")
        [world $]))

(defn post-log
  "Add a chat message from the given player."
  [world :- Game, player :- s/Str, body :- s/Str] :- EventResult
   (as-> world $
         (log $ {:name player :text body}
           player "You say, \"{{text}}\""
           :else "{{name}} says, \"{{text}}\"")
         [$ (r/response nil)]))
