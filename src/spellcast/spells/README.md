spell notes go here

dispel magic has

(interact 'any always)

and has special text for counterspell, magic mirror, and for disintegrating monsters and monster corpses
for everything else it just says "the %s is dispelled"
it also has a "the magical energies in the arena fade away" message -- not sure if this should be printed before or after interaction.

Perhaps we have default handlers for some outcomes --

(comment
  "Interact defines an interaction handler for the current spell; when this spell undergoes interaction checking it will be checked against every spell after it in the buffer, passed itself and the other spell and expected to return whatever ends up in the new spell buffer -- which may not map 1:1 to the inputs."
  (interact SPELL PREDICATE
    [self-arg other-arg]
    body)
  "PREDICATE is called with two args, self-arg and other-arg -- it's passed both for predicates like same-target?, but for some, like spell? it only checks other-arg.")


(defn invoke
  [world spell]
  ; lots of really nasty logging goes here to produce the
  ; "{{caster}} casts {{spell}} (with the {{hand}}) at {{target}}."
  ; log message
  )

(defn countered
  [world spell]
  (log world "The {{spell}} is destroyed by the Counter-Spell around {{target}}."))

(defn dispelled
  [world spell]
  (log world "The {{spell}} is dispelled."))

There is a complication here for stuff like Firestorm. Dispel Magic can just flag every other spell as "dispelled", but Firestorm et al can do different things to different targets depending on whether they're shielded, resistant, etc.

Offhand I can think of two ways to do this:
- flag individual players as shielded, resistant, etc and then check that in (resolve). This means it doesn't fit into the current interaction framework but has the virtue of simplicity.
- let (invoke) modify the contents of the spell buffer, and insert a separate effect for each player that does the thing. This is actually not too different from how zarfcast does it; for each player, during spell resolution, it checks if there is a storm "in the arena" and then prints either the countered, resistant, or spicy version of the message.

Ok, to load a spell-as-namespace, we do something like:
requiring-resolve 'spellcast.spells.spellname/properties to get the var for the spellprops
var-get on that to get the actual value
stick the ns in there somewhere?
merely loading the ns ensures that we have the interact handlers available
we also need to fetch the invoke, resolve, and any other handlers out of it
(TODO: do we make handlers for different outcomes a top-level thing, or is resolve responsible for checking flags on the spell or the target itself?)
