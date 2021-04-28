(ns spellcast.spells.surrender
  "This is not a spell; consequently, it cannot be cast at anyone, nor can it be dispelled, counter-spelled, reflected off a mirror, or delayed.
  A wizard who makes two simultaneous P gestures, irrespective of whether they terminate spells or not, surrenders and the contest is over. The surrendering wizard is deemed to have lost unless their gestures complete spells
  which kill their remaining opponent(s). Two simultaneous surrenders count as a draw. It is a necessary skill for wizards to work their spells so that they never accidentally perform two P gestures simultaneously.
  Wizards can be killed as they surrender (if hit with appropriate spells or attacks), but the referees will cure any diseases, poisons, etc. immediately after the surrender for them."
  (:refer-clojure :exclude [def defn defmethod defrecord fn letfn])
  (:require [clojure.pprint :refer [pprint]])
  (:require [schema.core :as s :refer [def defn defmethod defrecord defschema fn letfn]])
  (:require [taoensso.timbre :as timbre
             :refer [trace debug info warn error fatal
                     tracef debugf infof warnf errorf fatalf]])
  (:require
    [spellcast.data.game :as game]
    [spellcast.data.spell :refer [spell]]
    [spellcast.logging :refer [log]]
    ))

(comment
  "Surrender is a bit of an odd duck. It's not a spell, and if 'cast' by a player it is inserted into the spell buffer in addition to any spells they cast this turn. It doesn't interact with other spells and counts as a loss for the caster, with the caveat that if, on the turn in which they surrender, they are the last wizard standing (i.e. all other players are dead), they still win, and if multiple wizards surrendered on the same turn (and everyone else died), the surrendering wizards tie.

  Note that surrender 'fires' at the *end* of the turn, so it's possible for you to surrender but still end up dead if e.g. your opponent completed a lightning bolt on the same turn. As soon as the turn ends the judges will whisk you to safety and (if necessary) heal you, though.

  Implementing this is a TODO; right now it just behaves as a loss. It looks like in zarfcast, it's implemented by flagging players as surrendered immediately, but not removing the alive flag until after the endgame checks; thus, a player who is surrendered but also alive surrendered on that turn.")

(spell "Surrender"
  :type :immediate) ; bypasses spell selection and is entered into the spell list instantly

; "%s makes the gesture of surrender!"
; "Oh, dear. You appear to have surrendered."
; "%s has surrendered to %s!" ; two players, one surrenders
; "All of %s's opponents have surrendered!" ; >2 players surrendered on the last turn
; "%s finds that there is nobody left to surrender to!" ; last survivor surrenders on the turn everyone else dies
; "The survivors have all surrendered to each other!" ; some dead, everyone else surrendered
; "Everyone has surrendered to each other!" ; everyone surrendered

(defn invoke [world {:keys [caster] :as self}]
  (log world self
    caster "Oh, dear. You appear to have surrendered."
    :else "{{caster}} makes the gesture of surrender!"))

(defn completed [world spell]
  (game/pset world (:caster spell) :hp 0))
