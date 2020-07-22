(require 'clojure.spec.alpha 'expound.alpha 'io.aviso.repl)
(io.aviso.repl/install-pretty-exceptions)
; (set! clojure.spec.alpha/*explain-out* expound.alpha/printer)

(ns spellcast.core
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [io.aviso.repl]
    [spellcast.routes :as routes]
    [spellcast.world :as world]
    ))

(def log-levels
  {:trace  "TRC"
   :debug  "DBG"
   :info   "INF"
   :warn   "WRN"
   :error  "ERR"
   :fatal  "FTL"
   :report "RPT"})

(defn init-logging! []
  (timbre/merge-config!
    {;:output-fn #(-> % :msg_ deref str)
     :output-fn
     (fn logger [data]
       (let [{:keys [level #_vargs msg_ ?ns-str ?file
                     timestamp_ ?line]} data]
         (str
           (log-levels level) " "
           (force timestamp_) " "
           "[" (or ?ns-str ?file "?") ":" (or ?line "?") "] - "
           (force msg_)
           )))
     :timestamp-opts {:pattern "HH:mm:ss"}}))

(defn init []
  (io.aviso.repl/install-pretty-exceptions)
  (s/set-fn-validation! true)
  (init-logging!)
  (info "spellcast is starting")
  (world/reset-game!)
  (world/log! {} :all "Game starts."))

(defn destroy []
  (info "spellcast is shutting down"))


(def request-handler #'routes/handler)
