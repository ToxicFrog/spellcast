(ns spellcast.test.core)
(use 'clojure.test)
(require '[spellcast.net :as net]
         '[spellcast.game :as game]
         '[spellcast.game.common :refer :all]
         '[spellcast.spells :refer [available-spells]]
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
          players (doall (map (partial mock-player 8666) ["White Mage" "Black Mage" "Red Mage"]))
          endgame (async/<!! result)]
      (is (= 3 (-> endgame :players count)))
      (is (= "White Mage" (:name (get-player endgame "White Mage"))))
      (is (= "Black Mage" (:name (get-player endgame "Black Mage"))))
      (is (= "Red Mage" (:name (get-player endgame "Red Mage"))))
      (let [wm (get-player endgame "White Mage")]
        (is (= '(:f :f :f) (:left wm)))
        (is (= '(:p :p :p) (:right wm)))
        (is (= {:left [[:paralysis false]], :right [[:shield false]]}
               (available-spells (:left wm) (:right wm)))))
      )))
