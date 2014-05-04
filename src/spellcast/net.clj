(ns spellcast.net)
(require '[clojure.core.async :as async :refer [<! <!! >! >!! thread close! chan]]
         '[clojure.java.io :refer [reader writer]]
         '[clojure.edn :as edn]
         '[taoensso.timbre :as log])
(import '[java.net ServerSocket]
        '[java.io PrintWriter PushbackReader])

(defn socket-reader [id sock to]
  (let [ch (chan)
        reader (-> sock reader PushbackReader.)]
    (thread
      (log/debugf "[%d] Reader active." id)
      (try
        (doseq [msg (repeatedly #(edn/read reader))]
          (assert (map? msg))
          (log/debugf "[%d] >> %s" id (pr-str msg))
          (>!! ch (assoc msg :from id)))
        (catch Throwable e
          (log/warnf e "[%d] Exception in socket reader" id)
          ;(.printStackTrace e)
          (close! to)))
      (log/debugf "[%d] Reader exiting." id))
    ch))

(defn socket-writer [id sock]
  (let [writer (-> sock writer (PrintWriter. true))
        ch (chan)]
    (thread
      (log/debugf "[%d] Writer active." id)
      (try
        (doseq [msg (->> (repeatedly #(<!! ch)) (take-while identity))]
          (log/debugf "[%d] Got message %s" id (str msg))
          (.println writer (str msg)))
        (catch Exception e
          (log/warnf e "[%d] Exception in socket writer" id))
        (finally
          (log/infof "[%d] Client disconnecting." id)
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
    (log/infof "Opening listen socket on port %d" port)
    (thread
      (loop [id 0]
        (log/debugf "Waiting for client %d" id)
        (if (not (.isClosed sock))
          (let [client (.accept sock)
                to (socket-writer id client)
                from (socket-reader id client to)]
            (log/infof "Accepted connection %d from %s" id (.getInetAddress client))
            (>!! ch {:sock sock :from from :to to})
            (recur (inc id)))
          (log/debugf "Server socket closed!"))))
    [ch sock]))
