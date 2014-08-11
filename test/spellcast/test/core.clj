(ns spellcast.test.core)
(use 'clojure.test)
(require '[spellcast.net :as net]
         '[spellcast.game :as game]
         '[spellcast.util :refer :all]
         '[clojure.core.async :as async]
         '[clojure.java.io :refer [reader writer]]
         '[taoensso.timbre :as log])
(import '[java.net Socket]
        '[java.io PrintWriter PushbackReader])

(defn- mock-player [port name]
  (let [socket (Socket. "localhost" port)
        writer (-> socket writer (PrintWriter. true))
        reader (-> socket reader)]
    (async/thread
      (doseq [line (line-seq reader)]
        (log/debugf "[%s] %s" name line)))
    (.println writer (pr-str (list :login name)))
    (.println writer (pr-str (list :chat "Hi, everybody!")))
    (.println writer (pr-str (list :ready true)))
    socket))

(deftest test-game
  (testing "basic three-player game"
    ; start server
    (let [game (game/new-game :min-players 3 :max-players 3 :allow-spectators false)
          sock (net/listen-socket game 8666)
          result (thread-call' game/run-game game)
          players (doall (map (partial mock-player 8666) ["White Mage" "Black Mage" "Red Mage"]))]
      (is (= 3 (-> result async/<!! :players count))))))
