(ns spellcast.spells)

;; This actually lists the spells in *reverse* order, since that's the
;; easiest way to test suffixes

(defmacro defspell
  [name gestures & rest]
  `(def ^:private ~name ~(reverse gestures)))

(defmacro defspells
  [name & spells]
  (defn make-spell [name gestures desc]
    (let [gestures (vec (map keyword gestures))]
         [gestures [(keyword name) desc]]))
  (let [spells (mapcat #(apply make-spell %) spells)]
    `(def ^:private ~name ~(apply hash-map spells))))

(defspells classic
  (lightning-bolt-2 [w d d c]
    "Strikes one target with lightning.")
  (time-stop        [s p p c]
    "Stops time for everyone else for one turn."))

;(println classic)

(def ^:private rev-spells {[:c :d :d :w] :lightning-bolt-2
                           [:c :f :f :f :s :d] :disease
                           [:c :p :p :s] :time-stop
                           [:c :s :s :w] :ice-storm
                           [:c :w :f :w :w :d] :raise-dead
                           [:c :w :w :p :w :p] :haste
                           [:c :w :w :s] :fire-storm
                           [:d :d :f :f :d] :lightning-bolt
                           [:d :d :s :p] :charm-monster
                           [:d :d :s :s :f] :fireball
                           [:dd :f :f :w :d] :blindness
                           [:d :f :p :w] :cause-heavy-wounds
                           [:d :s] :missile
                           [:d :s :s :s :f :p :w :p] :finger-of-death
                           [:d :w :f :w :w :d] :poison
                           [:d :w :s] :fear
                           [:f :d :s :p] :charm-person
                           [:f :f :f] :paralysis
                           [:f :p :s] :anti-spell
                           [:f :s :d] :confusion
                           [:p] :shield
                           [:pp] :surrender
                           [:p :f :s :s] :resist-cold
                           [:p :f :w] :cause-light-wounds
                           [:p :f :w :w] :resist-heat
                           [:p :p :d] :amnesia
                           [:p :p :w] :counter-spell
                           [:p :s :s :s :w :d] :delayed-effect
                           [:p :w :d :p] :remove-enchantment
                           [:p :w :w] :protection-from-evil
                           [:ss :ww :p :p] :invisibility
                           [:s :w :w] :counter-spell
                           [:s :w :w :s :c] :summon-elemental
                           [:ww :c] :magic-mirror
                           [:w :d :s :p :f :p :s] :permanency
                           [:w :f :d] :cure-light-wounds
                           [:w :f :s] :summon-goblin
                           [:w :f :s :p] :summon-ogre
                           [:w :f :s :p :f] :summon-troll
                           [:w :f :s :p :f :w] :summon-giant
                           [:w :d :d :c] :dispel-magic
                           [:w :p :f :d] :cure-heavy-wounds})

;; Doubled gestures may be interpreted as single ones.
(def ^:private equiv-gestures {:d :dd, :s :ss, :w :ww, :p :pp, :f :ff})

(defn- spell-matches [gestures spell-code]
  (and (>= (count gestures) (count spell-code))
       (every? true? (map (fn [a b] (or (= a b)
                                        (= a (equiv-gestures b :undoubled))))
                          gestures spell-code))))

(defn- one-hand-sequence [gestures]
  (let [suffix (into () gestures)]
    (for [[spell-code spell-name] rev-spells
          :when (spell-matches suffix spell-code)]
      (if (#{:ff :pp :ww :ss :dd :c} (first spell-code))
        [spell-name true]
        [spell-name false]))))

(defn- hand-seq [main other]
  (map (fn [m o]
         (cond
          (and (= m :c) (= o :c)) :c
          (and (= m o) (equiv-gestures m)) (equiv-gestures m)
          (= m :c) nil
          :else m))
       main other))

(defn available-spells [left right]
  {:left (into [] (one-hand-sequence (hand-seq left right)))
   :right (into [] (one-hand-sequence (hand-seq right left)))})
