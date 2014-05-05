(ns spellcast.net)
(require '[spellcast.util :refer :all]
         '[clojure.core.async :as async :refer [<! <!! >! >!! close! chan sub]]
         '[clojure.java.io :refer [reader writer]]
         '[clojure.edn :as edn]
         '[taoensso.timbre :as log])
(import '[java.net ServerSocket SocketException]
        '[java.io PrintWriter PushbackReader])

(defn socket-reader [id sock in out]
  (let [reader (-> sock reader PushbackReader.)]
    (try-thread
      (str "socket reader " id)
      (log/debugf "[%d] Reader active." id)
      (doseq [msg (repeatedly #(edn/read reader))]
        ; incoming messages are EDN lists with the format '(tag & args)
        ; we annotate them with their origin
        (assert (list? msg) "Malformed message from client.")
        (log/debugf "[%d] >> %s" id (pr-str msg))
        (>!! in (with-meta msg {:id id}))
        (log/debugf "[%d] => %s" id (pr-str msg)))
      (catch SocketException e
        (log/infof "[%d] Connection error." id))
      (finally
        (>!! out (message id '(close)))
        (log/debugf "[%d] Reader exiting." id)))))

(defn socket-writer [id sock in outbus]
  (let [writer (-> sock writer (PrintWriter. true))
        ch (chan)]
    (sub outbus id ch)
    (sub outbus :all ch)
    (try-thread
      (str "socket writer " id)
      (log/debugf "[%d] Writer active." id)
      (doseq [msg (->> (repeatedly #(<!! ch))
                       (take-while #(not= :close (first %))))]
        (log/debugf "[%d] << %s" id (str msg))
        (.println writer (pr-str msg)))
      (catch SocketException e
        (log/infof "[%d] Connection error." id))
      (finally
        (log/infof "[%d] Client disconnecting." id)
        (>!! in (message id '(disconnect)))
        (.close sock)))))

(defn listen-socket
  "Create a listen socket and accept connections on it. Returns [channel socket].

  For each connection accepted, returns a map {:sock :from :to}, where sock is
  the socket itself and from and to are channels providing input from and output
  to the socket.

  A background thread is created for the listener (and another two for each
  connection accepted). This thread exits when the socket is closed."

  [game port]
  (let [sock (ServerSocket. port)]
    (log/infof "Opening listen socket on port %d" port)
    (try-thread
      "acceptor loop"
      (loop [id 0]
        (log/debugf "Waiting for client %d" id)
        (if (not (.isClosed sock))
          (let [client (.accept sock)]
            (socket-writer id client (:in game) (:out-bus game))
            (socket-reader id client (:in game) (:out game))
            (log/infof "Accepted connection %d from %s" id (.getInetAddress client))
            (recur (inc id)))))
      (catch SocketException e
        (log/info "Listen socket closed.")))
    (log/debugf "Listener socket running.")
    (assoc game :socket sock)))
