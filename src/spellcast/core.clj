(ns spellcast.core (:gen-class))
(use 'spellcast.game)
(require '[spellcast.net :as net]
         '[clojure.core.async :as async]
         '[taoensso.timbre :as log])

(defn -main
  [& args]
  (log/set-level! :info)
  (log/infof "Spellcast starting up.")
  (let [game (new-game :min-players 2 :max-players 2 :allow-spectators false)
        sock (net/listen-socket game 8666)]
    (async/<!! (run-game game))
    (.close sock)))
