(ns spellcast.test.core)
(use 'clojure.test)
(require '[spellcast.net :as net]
         '[spellcast.game :as game]
         '[spellcast.util :refer :all]
         '[clojure.core.async :as async]
         '[clojure.java.io :refer [reader writer]])
(import '[java.net Socket]
        '[java.io PrintWriter PushbackReader])

(defn- mock-player [port name]
  (let [socket (Socket. "localhost" port)
        writer (-> socket writer (PrintWriter. true))
        reader (-> socket reader PushbackReader.)]
    (.println writer (pr-str (list :login name)))
    (.println writer (pr-str (list :ready true)))
    socket))

(deftest test-game
  (testing "basic two-player game"
    ; start server
    (let [game (game/new-game :min-players 3 :max-players 3 :allow-spectators false)
          sock (net/listen-socket game 8666)
          result (thread-call' game/run-game game)
          p1 (mock-player 8666 "White Mage")
          p2 (mock-player 8666 "Black Mage")
          p3 (mock-player 8666 "Red Mage")]
      (is (= 3 (-> result async/<!! :players count))))))
