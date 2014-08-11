(ns spellcast.util)
(require '[clojure.core.async :as async]
         '[taoensso.timbre :as log])

(defmacro try-thread [name & body]
  (if (and (list? (last body)) (->> body last first (= 'finally)))
    `(async/thread
       (try
         ~@(butlast body)
         (catch Throwable e#
           (log/errorf e# "Uncaught exception in thread " ~name)
           (System/exit 1))
         ~(last body)))
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
