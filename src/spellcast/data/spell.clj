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
  During configure-spells, the options are processed. Each one has a :domain function (returns the set of possible values) and a :prefer function for the default value (if not present (first domain) is used). These functions take two options (the world state and the annotated spell). If :domain returns only one value, this is the simple case and that becomes the option's value. If it returns multiple values, we need to ask a question of the user and fill in the value based on their response.
  Once the options are processed, the {option-name option-value} map resulting from this is merged into the spell. It is an error for this to overwrite the 'standard' fields for the spell (:name, :caster, etc) -- TODO: enforce this in the SpellPage schema using (s/constrained).
  At this point, the spell is fully realized and ready for execution. It is placed into the spell buffer and its :invoke handler is called. This handler should just log the 'foo casts bar at baz with the moby hand' messages; since it is invoked before any spell interactions are processed, we don't yet know if the spellcasting even succeeds at this point. The buffer is then sorted by spell priority. (TODO: if interactions are all declared properly we might not need to sort it?)
  Once the buffer is populated, spell interactions occur. These are stored in :interactions and declared with the (spell/interact) macro. Each spell is checked against all the spells after it in the buffer, and the interaction handler is expected to return one of:
    nil -- do not modify self or other
    [self'] -- modify self, drop other from the cast buffer
    [self' & rest] -- modify self, insert rest into the cast buffer
  In particular, this can be used to delete the spells interacted with (but not self!), or insert new spells; for example, summon elemental can delete all other elemental summoning spells from the buffer and optionally update itself to be a no-op (if it cancelled rather than merging), while magic mirror can hack the target of any affected spell.
  The output of the interaction pass is a new spell execution queue with spells deleted, added, and/or modified; this is then consumed and the :finalize handler for each spell is called. In many cases the actual resolution was handled during the interaction pass, so the :finalize handler just displays flavour text.
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

(defschema OptionLister
  (s/=> [SpellTarget], Game s/Any))

(defschema SpellOption
  "A configuration point for a spell. Contains information about how to display it to the player when asking them to configure it, what the set of allowable options for it are, and how to pick the default."
  {; Player-visible question, like "who do you want to cast the spell on?"
   ; Can be templated with {{spell}}, {{caster}}, and {{hand}}.
   :question s/Str
   ; Domain of possible values for the parameter; should return a collection
   ; of valid targets when passed the game state.
   ; We declare it as taking Any here instead so we don't need to mess around
   ; with mutually recursive schemas.
   :domain OptionLister
   ; Collection of preferred values, e.g. Shield can be cast on any living thing
   ; but prefers to target the caster, while Missile prefers to target enemy wizards.
   ; If it contains more than one value the first is used as the default.
   :prefer OptionLister
   })

(defschema SpellGesture
  "Gestures that can appear in a spell definition (as opposed to gestures that can appear in a player's gesture history)."
  (s/enum
    :knife :palm :fingers :snap :digit :wave
    :palm2 :fingers2 :snap2 :digit2 :wave2 :clap2))

(defschema SpellPage
  "A spell, as it exists in the spellbook."
  {:name s/Str
   :id s/Keyword
   :type s/Keyword ; TODO schema for spell types
   :priority s/Int
   :gestures [SpellGesture]
   ; Spell parameter questions.
   ; Each one contains information about the parameter that will be reified into
   ; actual configuration settings for the spell after it is selected and before
   ; execute-spells.
   :options {s/Keyword SpellOption}
   ; Spell/spell interactions.
   ; If a spell defines any interactions, then after spell selection and before
   ; spell resolution, those interactions will be tested against all other spells
   ; in the execution buffer.
   :interactions [s/Any]
   ; Implementation functions. All have signature (Game Spell => Game) and are
   ; passed the fully reified version of the spell, with caster, handedness, and
   ; parameters filled in.
   ; Called when spellcasting begins. Typically just outputs a "{{caster}} casts
   ; {{spell}}" message. Kept separate from the resolution function so that metamagic
   ; spells can modify one without touching the other. This is called after spell
   ; selection but before interactions run.
   :invoke (s/=> Game, Game s/Any)
   ; Called when spellcasting completes, after interactions are resolved. This
   ; should perform the actual effects of the spell, but metamagic might replace
   ; it with e.g. "the spell is captured by the Delayed Effect" or "the missile
   ; shatters on the shield" or the like.
   :finalize (s/=> Game, Game s/Any)
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

(defn- spell-id []
  (->> (ns-name *ns*)
       name reverse
       (take-while (partial not= \.))
       reverse (apply str)
       keyword))

(defmacro spell
  "Define a spell. The spell ID is inferred from the namespace this is invoked in (which means you cannot define multiple spells in the same namespace; pull the common elements into their own ns and require them). Tail elements are assoc'd into the spell definition unmodified.
  Use the (option) and (interact) macros to define caster-settable spell parameters and spell/spell interaction handlers."
  [name & fields]
  (let [spell-id (spell-id)]
    (println "Creating spell " spell-id " named " name)
    `(do
       (def ~'properties (assoc {:id ~spell-id :name ~name} ~@fields))
       (def ~'options {})
       (def ~'interactions []))))

(defmacro option
  "Define an option (a caster-configurable spell parameter) such as target or element. *name* is the keyword used to look up the option, *desc* is the player-visible description.
  The fields :domain and :prefer are mandatory; the former should be a function that returns the set of allowable values for the option, and the latter a function that returns the most preferred value (or a seq of preferable values, most preferable first)."
  [name question & fields]
  `(def ~'options
     (assoc ~'options ~name ~(apply assoc {:question question} fields))))

(defn- should-interact?
  "Check if spell *self* should interact with spell *other*, based on id and pred."
  [self other id pred]
  (and
    (or (= id :any) (= id (:id other)))
    (pred self other)))

(defmacro interact
  "Define a spell/spell interaction.
  Other-spell-id should be the spell id to interact with, or :all to interact with all other spells.
  pred should be a predicate on [self other] that allows for further filtering, such as same-target?.
  Given arguments [self other] (where *self* is the interacting spell and *other* is the spell being interacted with, it should return either nil (meaning that neither self nor other should be affected by the interaction), or [self' & others], where self' is the updated value of self and others are the (possibly empty) vec of spells that should be queued instead of other."
  [other-spell-id pred [self other :as args] & body]
  (let [spell-id (spell-id)
        other-spell-id (if (keyword? other-spell-id) other-spell-id `#{~other-spell-id})]
    `(def ~'interactions
       (conj ~'interactions
         (fn ~args
           (info "Checking for interaction between" ~spell-id "and" (:id ~other))
           (info "Interaction key:" ~other-spell-id)
           (info "Interaction pred:" ~pred)
           (when (~should-interact? ~self ~other ~other-spell-id ~pred)
             (info "Interacting" ~spell-id "with" ~other-spell-id)
             ~@body))))))

(defn spell? [self other]
  (= :spell (:type other)))

(defn same-target? [self other]
  (and (some? (:target self))
    (= (:target self) (:target other))))

(defn all-of [& preds]
  (fn [self other]
    (->> preds
         (map #(% self other))
         (reduce #(and %1 %2)))))
