(ns spellcast.test.core)
(use 'clojure.test)
(require '[spellcast.net :as net]
         '[spellcast.game :as game]
         '[spellcast.game.common :refer :all]
         '[spellcast.spells :refer [available-spells]]
         '[spellcast.util :refer :all]
         '[spellcast.test.mock-player :refer :all]
         '[clojure.core.async :as async]
         '[clojure.java.io :refer [reader writer]]
         '[taoensso.timbre :as log])
(import '[java.net Socket]
        '[java.io PrintWriter PushbackReader])

(defmacro test-gestures [player left right]
  (is (= left (take (count left) (player :left))))
  (is (= right (take (count right) (player :right)))))

(defmacro test-spells [player left right]
  (is (= {:left left :right right}
         (available-spells (player :left) (player :right)))))

(deftest test-game
  (testing "basic three-player game"
    ; start server
    ; we have 1, 2, 3, 4, and 5-gesture spell cycles, so we need to run the game
    ; for 60 turns to check them all at once!
    (let [game (game/new-game :min-players 3
                              :max-players 3
                              :allow-spectators false
                              :turn-limit 60)
          sock (net/listen-socket game 8666)
          result (thread-call' game/run-game game)
          wm (mock-player 8666 "White Mage" (repeat :p) (cycle [:d :f :w]))
          bm (mock-player 8666 "Blck  Mage" (cycle [:s :d]) (cycle [:f :s :s :d :d]))
          rm (mock-player 8666 "Red   Mage" (cycle [:s :w :w :c]) (cycle [:w :s :s :c]))
          endgame (async/<!! result)]
      (is (= 3 (-> endgame :players count)))
      (is (= 60 (endgame :turn)))
      (is (= "White Mage" (:name (get-player endgame "White Mage"))))
      (is (= "Blck  Mage" (:name (get-player endgame "Blck  Mage"))))
      (is (= "Red   Mage" (:name (get-player endgame "Red   Mage"))))
      ; WM should be casting Shield and Cure Light Wounds
      ; BM should be casting Missile and Fireball
      ; RM should be casting Firestorm and Icestorm
      (let [wm (get-player endgame "White Mage")]
        (test-gestures wm
                       (take 8 (repeat :p))
                       '(:w :f :d :w :f :d))
        (test-spells wm
                     [[:shield false]]
                     [[:cure-light-wounds false]]))
      (let [bm (get-player endgame "Blck  Mage")]
        (test-gestures bm
                       '(:d :s :d :s :d :s)
                       '(:d :d :s :s :f))
        (test-spells bm
                      [[:missile false]]
                      [[:fireball false]]))
      (let [rm (get-player endgame "Red   Mage")]
        (test-gestures rm
                       '(:c :w :w :s)
                       '(:c :s :s :w))
        (test-spells rm
                     [[:fire-storm true]]
                     [[:ice-storm true]]))
      )))
