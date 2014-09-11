(ns spellcast.test.mock-player)

(require '[spellcast.net :as net]
         '[spellcast.game :as game]
         '[spellcast.game.common :refer :all]
         '[spellcast.spells :refer [available-spells]]
         '[spellcast.util :refer :all]
         '[clojure.core.async :as async]
         '[clojure.edn :as edn]
         '[clojure.java.io :refer [reader writer]]
         '[taoensso.timbre :as log])
(import '[java.net Socket]
        '[java.io PrintWriter PushbackReader])

(defn- prefix-equal [xs ys]
  (or (empty? xs) (empty? ys)
      (and (= (first xs) (first ys))
           (prefix-equal (rest xs) (rest ys)))))

(defn- send [name writer & args]
  (log/debugf "[%s] >> %s" name (pr-str args))
  (.println writer (pr-str (apply list args))))

(defn- wait [name reader & args]
  (loop [msg (edn/read reader)]
    (log/debugf "[%s] << %s" name (pr-str msg))
    (if (not (prefix-equal msg args))
      (recur (edn/read reader))
      (log/debugf "[%s] OK %s" name (pr-str args)))))

; need to create a channel here that the main mock listens on
; main mock needs to be forked into its own thread, so there's two threads,
; one for the mock proper and one for the background reader
; background reader sends messages on channel
; main mock thread handles (wait) by reading from that channel until it finds
; what it's looking for
; channel needs to be able to buffer messages -- can it do this?
(defn mock-player [port name left right]
  (let [socket (Socket. "localhost" port)
        writer (-> socket writer (PrintWriter. true))
        reader (-> socket reader PushbackReader.)
        send (partial send name writer)
        wait (partial wait name reader)]
    ; start the writer
    (try-thread (str "mock-player/writer " name)
      ; send login info
      (send :login name)
      (send :ready true)
      (doseq [[left right] (map list left right)]
        (wait :turn)
        (send :gestures left right)
        (send :ready true)))))
