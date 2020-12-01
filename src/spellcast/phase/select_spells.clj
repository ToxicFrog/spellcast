(ns spellcast.phase.select-spells
  "Figures out what spells each player is casting (if any). If there is no ambiguity this merely populates the spell buffer and continues to execute-spells. If there is, it queries the players to find out which spells they intended to cast, and fills the spell buffer based on that."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.game :as game]
    [spellcast.data.player :as player]
    [spellcast.logging :refer [log]]
    [spellcast.phase.common :as phase-common :refer [defphase]]
    ))

(comment
(defschema TargetFilter
  (s/=> Player s/Bool))

(declare SpellFn)

(defschema Spell
  "A spell, as it exists in the spellbook."
  {:name s/Str
   ; Filtered over the set of possible targets in the world to produce the set of valid targets.
   :target TargetFilter
   ; Filtered over the set of possible targets in the world to produce the default target.
   ; The first value that matches the filter will be used as the default, if there are more than one.
   ; This can be used as, e.g., (spell "Counterspell" :target living? :default caster)
   (s/optional-key :default) TargetFilter
   ; The spell priority. Spells with lower priority go earlier in the execution phase.
   :priority s/Int
   ; The spell implementation.
   ; Is passed the world state and
   :on-cast (recursive #'SpellFn)
   })

(defschema SpellBuffer [Spell])

(defschema SpellFn
  (s/=> [Game SpellBuffer], Game SpellBuffer (recursive #'Spell)))

);comment

(defn- spell [name gestures & rest]
  (apply assoc {:name name :gestures gestures} rest))

; TODO everything here is kind of a mess with no clear idea of where we use caster/target
; names and where we use the actual objects.
; In prod we'll need to use names so that we can look up the current version of each
; in the world, but this also means we need careful handling for monsters & elementals
; since we can't look them up in the player table.
; possibly we should move to entity IDs and just index by that, no lookups by name
; at all, session map just holds an EID?
(defn- default-invoke-message
  [world {:keys [caster hand params name] :as spell}]
  (let [handedness (condp = hand
                     :left "the left hand"
                     :right "the right hand"
                     :both "both hands")
        target (:target params)
        target-str (cond
                 (= :everything target) "over the arena"
                 (= :nothing target) "up into the air"
                 :else (str "at " target))
        caster-obj (game/get-player world caster)
        log-params {:spell name :handedness handedness :caster caster :target target-str
                    :caster-obj caster-obj}]
    (info "Writing invokation messages for" (:name spell) "into log...")
    (cond
      (= caster target)
      (log world log-params
        caster "You cast {{spell}} (with {{handedness}}) at yourself."
        :else "{{caster}} casts {{spell}} (with {{handedness}}) at {{pronoun caster-obj :ref}}.")
      ; HACK HACK HACK -- one wizard casting at another
      ; in future this needs to properly handle casting at monsters, etc.
      (string? target)
      (log world log-params
        caster "You cast {{spell}} (with {{handedness}}) {{target}}."
        target "{{caster}} casts {{spell}} (with {{handedness}}) at you."
        :else "{{caster}} casts {{spell}} (with {{handedness}} {{target}}.")
      ; casting into the air/over the arena
      (keyword? target)
      (log world log-params
        caster "You cast {{spell}} (with {{handedness}}) {{target-str}}."
        :else "{{caster}} casts {{spell}} (with {{handedness}} {{target}}.")
      :else
      (log world log-params
        :all "Something went horribly wrong.")
      )))

(defn- default-resolve
  [world spell]
  world)

(defn- self [world spell]
  (info "self-targeting" (:caster spell))
  (:caster spell))

(defn- enemy [world spell]
  (let [caster (spell :caster)]
    (->> world :players vals
        (filter #(not= caster (:name %)))
        first
        :name)))

(def spellbook
  [(spell "Surrender" [:palm2]
     :target self
     :invoke default-invoke-message
     :resolve (fn [world spell]
                (pprint spell)
                (let [target (-> spell :params :target)]
                  (-> world
                      (assoc-in [:players target :hp] 0)
                      (log {:target target}
                        target "You have surrendered!"
                        :else "{{target}} has surrendered!")))))
   (spell "Shield" [:palm]
     :target self
     :invoke default-invoke-message
     :resolve default-resolve)
   (spell "Stab" [:knife]
     :target enemy
     :invoke default-invoke-message
     :resolve default-resolve)
   ])

(defn potential-spells
  "Simple test function to turn gesture sequences into spells. Only ever returns one spell, only looks at the most recent gesture."
  [gestures]
  (get spellbook (first gestures)))

(defn- doubled? [gesture] (->> gesture name last (= \2)))
(defn- doubled [gesture] (keyword (str (name gesture) "2")))
(defn- undoubled [gesture]
  (if (doubled? gesture)
    (->> gesture name drop-last (apply str) keyword)
    gesture))

(defn- canonicalize-gestures [{:keys [left right] :as gestures}]
  (if (= left right)
    {:left (doubled left) :right (doubled right)}
    gestures))

(defn- one-hand-history
  "Extract gesture information from the player in a format suitable for spell matching. Returns the history for the given hand, most recent gesture first, with doubled gestures canonicalized (i.e. a clap with both hands will be returned as :clap2)."
  [player hand]
  (->> player
       :gestures
       (take 8)
       (map canonicalize-gestures)
       (map hand)))

(defn- spell-matches?
  "Returns true if the spell matches the given gesture history."
  [history spell]
  (let [recipe (-> spell :gestures reverse)]
    (println "Checking for spell match between"
      (:name spell) recipe "and" history)
    (and
      ; gesture history needs to be at least as long as spell recipe
      (>= (count history) (count recipe))
      ; every gesture in the history needs to either be identical to the corresponding
      ; gesture in the recipe, or be the doubled version of it
      (every? true? (map (fn [h r] (or (= h r)
                                     (= h (doubled r))))
                      history recipe)))))

(defn- two-handed? [spell]
  (-> spell :gestures last doubled?))

(defn- which-spell
  "Return the spell the given hand is casting."
  [player hand]
  (let [history (one-hand-history player hand)
        spell (first (filter (partial spell-matches? history) spellbook))]
    (println "which spell?" (:name player) hand spell)
    (cond
      (nil? spell) nil
      (two-handed? spell) (assoc spell :hand :both)
      :else (assoc spell :hand hand))))

; In the final version this will take into account any questions the player has
; answered, and there will be a separate step in BEGIN that builds the question buffers.
(defn- which-spells
  "Return the set of spells the given player is casting."
  [player]
  (let [left-spell (which-spell player :left)
        right-spell (which-spell player :right)]
    (cond
      ; special
      (= nil left-spell right-spell) []
      (= nil left-spell) [right-spell]
      (= nil right-spell) [left-spell]
      ; ok, if we get this far they're either casting two spells, or one spell with both hands...
      ; in the final version this is going to be grody as far as questions go, because we can end up
      ; with situations like: left P P W S, right W W W S
      ; which is counterspell with the right, and invisibility with the left (with both)
      ; in this version, though, there's no overlap like that
      (= :both (left-spell :hand)) [left-spell]
      :else [left-spell right-spell])))

(defn- reify-spell
  [world caster spell]
  (let [spell (assoc spell :caster caster)]
    (assoc-in spell [:params :target] ((:target spell) world spell))))

(defn- prepare-spells
  "Prepare a player's spells for casting. Takes a player with gestures ready, returns a player with spells buffered.
  Note that the gestures are in LIFO order, i.e. reversed from how they appear in the spellbook.
  This is really quick and dirty and needs to be replaced for the final version."
  [world player]
  (let [spells (map (partial reify-spell world (:name player)) (which-spells player))
        world' (update world :casting concat spells)]
    (println "prepare spells" spells)
    world'))
  ; (update world :casting conj (which-spells player)))

(defphase select-spells
  (reply BEGIN [world]
    ; In the full version this will check for ambiguous spells and populate the
    ; question buffers based on that, as well as presenting questions like Delayed
    ; Effect that could potentially insert things into the spell buffers.
    world)

  (reply END [world]
    ; questions have all been answered, so fill the spell buffers based on that
    ; and clear the question buffers.
    (game/reduce-players world prepare-spells))
    ; (let [world' (game/reduce-players world prepare-spells)]
    ;   (pprint (dissoc world' :log))
    ;   world'))

  (reply INFO [world]
    {:when-ready "Waiting for opponents..."
     :when-unready "Submit Answers"})

  (reply NEXT [world]
    ; In the full version, this will proceed iff all disambiguation questions have
    ; been answered. In this version, there are no disambiguation questions so we
    ; can proceed immediately.
    ; Similarly, there is no separate spell configuration step, so we skip configure-spells
    ; and proceed directly to execute-spells.
    (when (game/all-players? world :ready)
      :execute-spells))

  ; event handlers for Q&A go here

  (event ready [world player ready?]
    (game/pset world player :ready ready?)))
