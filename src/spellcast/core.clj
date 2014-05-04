(ns spellcast.core (:gen-class))
(use 'spellcast.game)
(require '[spellcast.net :as net]
         '[clojure.core.async :as async]
         '[taoensso.timbre :as log])

(defn -main
  [& args]
  (log/infof "Spellcast starting up.")
  (-> (new-game :min-players 2 :max-players 2 :allow-spectators false)
      (net/listen-socket 8666)
      run-game
      async/<!!))
