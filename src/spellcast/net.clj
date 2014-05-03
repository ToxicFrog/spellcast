(ns spellcast.net)
(require '[clojure.core.async :as async :refer [<! <!! >! >!! thread close! chan]]
         '[clojure.java.io :refer [reader writer]])
(import '[java.net ServerSocket]
        '[java.io PrintWriter])

(defn socket-reader [sock]
  (let [ch (chan 8)]
    (thread
      (println "Reader for " sock " online")
      (try
        (doseq [line (-> sock reader line-seq)]
          (>!! ch line))
        (catch Exception e
          (println "Exception in socket reader: " e)))
      (println "Reader for " sock " exiting"))
    ch))

(defn socket-writer [sock]
  (let [writer (-> sock writer (PrintWriter. true))
        ch (chan 8)]
    (thread
      (println "Writer for " sock " online")
      (try
        (doseq [msg (take-while identity (repeatedly #(<!! ch)))]
          (println "Got message for " sock ": " msg)
          (.println writer msg))
        (catch Exception e
          (println "Exception in socket writer: " e))
        (finally
          (println "Closing " sock)
          (.close sock))))
    ch))

(defn close-client [client]
  (close! (:from client))
  (close! (:to client))
  {:sock (:sock client)})

(defn listen-socket
  "Create a listen socket and accept connections on it. Returns [channel socket].

  For each connection accepted, returns a map {:sock :from :to}, where sock is
  the socket itself and from and to are channels providing input from and output
  to the socket.

  A background thread is created for the listener (and another two for each
  connection accepted). This thread exits when the socket is closed."

  [port]
  (let [sock (ServerSocket. port)
        ch (chan)]
    (thread
      (while (not (.isClosed sock))
        (let [client (.accept sock)
              from (socket-reader client)
              to (socket-writer client)]
          (>!! ch {:sock sock :from from :to to}))))
    [ch sock]))
