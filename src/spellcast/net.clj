(ns spellcast.net)
(require '[clojure.core.async :as async :refer [<! <!! >! >!! close! chan sub]]
         '[clojure.java.io :refer [reader writer]]
         '[clojure.edn :as edn]
         '[taoensso.timbre :as log])
(import '[java.net ServerSocket SocketException]
        '[java.io PrintWriter PushbackReader])

(defmacro try-thread [name & body]
  (if (->> body last first (= 'finally))
    `(async/thread
       (try
         ~@(butlast body)
         (catch Throwable e#
           (log/errorf e# "Uncaught exception in thread " ~name))
         ~(last body)))
    `(async/thread
       (try
         ~@body
         (catch Throwable e#
           (log/errorf e# "Uncaught exception in thread " ~name))))))

(defn socket-reader [id sock ch]
  (let [reader (-> sock reader PushbackReader.)]
    (try-thread
      (str "socket reader " id)
      (log/debugf "[%d] Reader active." id)
      (doseq [msg (repeatedly #(edn/read reader))]
        (assert (map? msg))
        (log/debugf "[%d] >> %s" id (pr-str msg))
        (>!! ch (assoc msg :from id)))
      (catch SocketException e
        (log/infof "[%d] Connection error." id))
      (finally
        (>!! ch {:tag id :close true})
        (log/debugf "[%d] Reader exiting." id)))
    ch))

(defn socket-writer [id sock outbus]
  (let [writer (-> sock writer (PrintWriter. true))
        ch (chan)]
    (sub outbus id ch)
    (sub outbus :all ch)
    (try-thread
      (str "socket writer " id)
      (log/debugf "[%d] Writer active." id)
      (doseq [msg (->> (repeatedly #(<!! ch)) (take-while #(not (:close %))))]
        (log/debugf "[%d] << %s" id (str msg))
        (.println writer (pr-str (dissoc msg :tag))))
      (catch SocketException e
        (log/infof "[%d] Connection error." id))
      (finally
        (log/infof "[%d] Client disconnecting." id)
        (.close sock)))))

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

  [port ch outbus]
  (let [sock (ServerSocket. port)]
    (log/infof "Opening listen socket on port %d" port)
    (try-thread
      "acceptor loop"
      (loop [id 0]
        (log/debugf "Waiting for client %d" id)
        (if (not (.isClosed sock))
          (let [client (.accept sock)
                to (socket-writer id client outbus)
                from (socket-reader id client ch)]
            (log/infof "Accepted connection %d from %s" id (.getInetAddress client))
            (recur (inc id))))))
    sock))
