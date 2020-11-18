(ns spellcast.frontend
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! go]]
            [clojure.string :as string]
            [oops.core :refer [oget oset! ocall!]]
            [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]]
            ; [taoensso.timbre :as timbre
            ;  :refer [trace debug info warn error fatal
            ;          tracef debugf infof warnf errorf fatalf]]
  ))

(enable-console-print!)
(prn "Starting up...")

(defn $ [& sel] (.querySelector js/document (apply str sel)))
(defn $$ [& sel] (.querySelectorAll js/document (apply str sel)))

(defn refresh [path stamp element render]
  (go
    (let [url (str path "/" stamp)
          response (<! (http/get url {:with-credentials? true}))
          body (response :body)]
      (if (response :success)
        (do
          (oset! element :innerHTML (render body))
          (js/setTimeout refresh 1 path (body :stamp) element render))
        (oset! element :innerHTML (str "Error loading " url " -- try reloading the page."))))))

(defn render-log [log]
  (string/join "<br/>" (log :log)))

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
  (refresh "/log" 0 ($ "#log") render-log)
  (ocall! ($ "#talk") :addEventListener "change" send-chat-message)
  (prn "Initialization complete."))

(set! js/initSpellcast init)
