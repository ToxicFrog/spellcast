(ns spellcast.phase.all
  "An empty namespace that does nothing but pull in all the other phases, to make sure that their event handlers are registered.

  The overall structure of the game is:
  - pregame
    - players can chat but not do much else
    - new players can join until a quorum is reached
  - collect-gestures
    - all active players need to send gestures and then signal readiness
  - select-spells
    - figure out which spell(s) each player is casting and buffer them
  - execute-spells
    - run the spells that were buffered in the previous phase
  - cleanup
    - determine if there is a winner, tick effects, etc
  - postgame
    - a winner has been chosen, players can chat for a bit before game exit"
  (:require [spellcast.phase.pregame]
            [spellcast.phase.collect-gestures]
            [spellcast.phase.select-spells]
            [spellcast.phase.execute-spells]
            [spellcast.phase.cleanup]
            [spellcast.phase.postgame]))
