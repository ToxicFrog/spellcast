(ns spellcast.util)
(require '[clojure.core.async :as async])

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
