(ns spellcast.frontend
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! go]]
            [clojure.string :as string]
            [oops.core :refer [oget oset! ocall!]]
            [reagent.core :as r]
            [reagent.dom]
            [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]]
            ; [taoensso.timbre :as timbre
            ;  :refer [trace debug info warn error fatal
            ;          tracef debugf infof warnf errorf fatalf]]
  ))

(enable-console-print!)
(prn "Starting up...")

(defn $ [& sel] (.querySelector js/document (apply str sel)))
(defn $$ [& sel] (.querySelectorAll js/document (apply str sel)))

(defn long-poll
  "Long-poll the given view path and whenever it changes, reset! the result into the atom. If called without a stamp, uses a stamp of 0; subsequent refreshes get the stamp from the X-Stamp header."
  ([path atom] (long-poll path atom 0))
  ([path atom stamp]
    (go
      (let [url (str path "/" stamp)
            response (<! (http/get url {:with-credentials? true}))
            success (response :success)]
        (prn response)
        (if success
          (do
            (reset! atom (response :body))
            (js/setTimeout long-poll 1 path atom (get-in response [:headers "x-stamp"])))
          (do
            (reset! atom nil)
            (js/setTimeout long-poll 5000 path atom 0)))))))


(defn log-view []
  (let [log (r/atom [])]
    (long-poll "/log" log)
    (fn []
      (if @log
        (into [:div] (interpose [:br] @log))
        [:div "Error loading log!"]))))

(defn show-gesture-picker [picker]
  (ocall! picker [:classList :remove] "hidden"))

(defn init-gesture-picker [player hand]
  (let [cell ($ "#gesture-" player "-" hand "-0")
        picker ($ "#gesture-picker-" hand)]
    (oset! cell :onclick #(show-gesture-picker picker))
    (oset! picker [:style :left] (str (oget cell :x) "px"))
    (oset! picker [:style :top] (str (oget cell :y) "px"))))

(defn post [path body]
  (http/post path {:with-credentials? true
                   :json-params body}))

(defn send-chat-message [event]
  (post "/game/log" {:text (oget event :target :value)})
  (oset! event [:target :value] ""))

(defn init [player]
  (init-gesture-picker player "left")
  (init-gesture-picker player "right")
  ; (refresh "/log" 0 ($ "#log") render-log)
  (reagent.dom/render [log-view] ($ "#log"))
  (ocall! ($ "#talk") :addEventListener "change" send-chat-message)
  (prn "Initialization complete."))

(set! js/initSpellcast init)
