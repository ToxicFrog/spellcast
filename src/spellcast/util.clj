(ns spellcast.util)
(require '[clojure.core.async :as async]
         '[taoensso.timbre :as log])

(defmacro try-thread
  "Run a thread in a try-catch block, and react to exceptions by logging them
  and then killing the program rather than the default behaviour, which is to
  swallow them and kill only that thread."
  [name & body]
  (if (and (list? (last body)) (->> body last first (= 'finally)))
    ; It has a "finally" clause that we should splice in after the catch.
    `(async/thread
       (try
         ~@(butlast body)
         (catch Throwable e#
           (log/errorf e# "Uncaught exception in thread " ~name)
           (System/exit 1))
         ~(last body)))
    ; No "finally".
    `(async/thread
       (try
         ~@body
         (catch Throwable e#
           (log/errorf e# "Uncaught exception in thread " ~name)
           (System/exit 1))))))

(defn get-meta [obj key]
  ((meta obj) key))

(defn message [id payload]
  (with-meta payload {:id id}))

(defn thread-call' [f & args]
  (try-thread (str f)
    (apply f args)))
