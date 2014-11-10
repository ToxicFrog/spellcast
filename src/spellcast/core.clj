(ns spellcast.core (:gen-class))
(require '[spellcast.net :as net]
         '[spellcast.util :refer :all]
         '[spellcast.game :refer :all]
         '[clojure.core.async :as async]
         '[taoensso.timbre :as log])

(defn -main
  [& args]
  (log/set-level! :info)
  (log/infof "Spellcast starting up.")
  (let [game (new-game :min-players 2 :max-players 2 :allow-spectators false)
        sock (net/listen-socket game 8666)]
    (async/<!! (thread-call' run-game game))
    (.close sock)))
