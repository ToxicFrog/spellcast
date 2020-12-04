(ns spellcast.data.spell
  "Types and functions for dealing with spells. See data.spellbook for types related to spell collections, including spell matching based on gestures, and spellbook.* for actual spell implementations."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.logging :refer [log]]
    [spellcast.data.player :as player :refer [Gesture Hand]]
    ))

(comment "
  ON THE LIFE CYCLE OF SPELLS

  This part is a bit complicated, so it deserves some explanation.
  A spell 'at rest' in the spellbook is represented by a SpellPage. This has a name, gestures, priority, event handlers (more on those later), and an options map, which may be empty.
  Once someone starts casting it -- i.e. once it has been chosen in the select-spells phase -- it gets partially reified and becomes a Spell. At this point it is annotated with :caster and :hand information and entered into the spell buffer, but it is not yet configured and thus not ready for execution.
  During configure-spells, the options are processed. Each one has a :domain function (returns the set of possible values) and an optional :default function for the default value (if not present (first domain) is used). These functions take two options (the world state and the annotated spell). If :domain returns only one value, this is the simple case and that becomes the option's value. If it returns multiple values, we need to ask a question of the user and fill in the value based on their response.
  TODO: I may want to combine :domain and :default into a single function and just say 'the default is whatever :domain returns first'.
  Once the options are processed, the {option-name option-value} map resulting from this is merged into the spell. It is an error for this to overwrite the 'standard' fields for the spell (:name, :caster, etc) -- TODO: enforce this in the SpellPage schema using (s/constrained).
  At this point, the spell is fully realized and ready for execution. The execute-spell phase uses the two event handlers in the spell, :invoke and :resolve, for this. Both are passed the world and the fully annotated spell as arguments.
  The former is used at the start of casting and should only be used to log the 'foo casts bar at baz' messages and the like; any change it makes to the world state should be a change that it makes sense to make *whether or not the spellcasting succeeds*.
  The latter is then used to apply any state changes made by the spell. Some spells, like metamagic, may wrap or replace this function to do something completely different.
  ")

(def SpellTarget
  "A placeholder for an entity ID used to denote a spell target. This might get replaced with some other type later."
  s/Str)

(defschema CastingHand
  "What hand is being used to cast a spell."
  (s/enum :left :right :both))

(defn hand-str [hand :- CastingHand]
  (get {:left "the left hand"
        :right "the right hand"
        :both "both hands"}
    hand
    "?your tentacles?"))

(def Game
  (s/recursive #'spellcast.data.game/Game))

(defschema SpellOption
  "A configuration point for a spell. Contains information about how to display it to the player when asking them to configure it, what the set of allowable options for it are, and how to pick the default."
  {; Player-visible question, like "who do you want to cast the spell on?"
   ; Can be templated with {{spell}}, {{caster}}, and {{hand}}.
   :question s/Str
   ; Domain of possible values for the parameter; should return a collection
   ; of valid targets when passed the game state.
   ; We declare it as taking Any here instead so we don't need to mess around
   ; with mutually recursive schemas.
   :domain (s/=> [SpellTarget], Game s/Any)
   ; Default value for the parameter. If absent (first domain) is used. This can
   ; be used to, e.g., select the caster by default for defensive spells while
   ; still allowing any living thing as the target.
   (s/optional-key :default) (s/=> SpellTarget, Game s/Any)
   })

(defschema SpellGesture
  "Gestures that can appear in a spell definition (as opposed to gestures that can appear in a player's gesture history)."
  (s/enum
    :knife :palm :fingers :snap :digit :wave
    :palm2 :fingers2 :snap2 :digit2 :wave2 :clap2))

(defschema SpellPage
  "A spell, as it exists in the spellbook."
  {:name s/Str
   :priority s/Int
   :gestures [SpellGesture]
   ; Spell parameter questions.
   ; Each one contains information about the parameter that will be reified into
   ; actual configuration settings for the spell after it is selected and before
   ; execute-spells.
   :options {s/Keyword SpellOption}
   ; Implementation functions. All have signature (Game Spell => Game) and are
   ; passed the fully reified version of the spell, with caster, handedness, and
   ; parameters filled in.
   ; Called when spellcasting begins. Typically just outputs a "{{caster}} casts
   ; {{spell}}" message. Kept separate from the resolution function so that metamagic
   ; spells can modify one without touching the other.
   :invoke (s/=> Game, Game s/Any)
   ; Called when spellcasting completes, typically immediately after :invoke. This
   ; should perform the actual effects of the spell, but metamagic might replace
   ; it with e.g. "the spell is captured by the Delayed Effect" or "the missile
   ; shatters on the shield" or the like.
   :resolve (s/=> Game, Game s/Any)
   })

(defschema Spell
  "A spell, as it exists when being cast."
  (-> SpellPage
      (dissoc :options) ; now optional
      (assoc
        :caster SpellTarget
        :hand CastingHand
        (s/optional-key :options) {s/Keyword SpellOption}
        ; configuration data, could be anything
        s/Keyword s/Any)))

; (defn default-invoke
;   "Default implementation of :invoke for use in spells that don't provide one."
;   [world :- Game, _spell :- Spell]
;   (log world {} :all "Placeholder invoke."))

(defn invoke [world :- Game, spell :- Spell]
  (let [invoke-fn (:invoke spell)]
    (invoke-fn spell world)))

(defn resolve [world :- Game, spell :- Spell]
  (let [resolve-fn (:resolve spell)]
    (resolve-fn spell world)))
