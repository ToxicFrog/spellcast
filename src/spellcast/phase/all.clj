(ns spellcast.phase.all
  "An empty namespace that does nothing but pull in all the other phases, to make sure that their event handlers are registered.

  The overall structure of the game is:
  - pregame
    - players can chat but not do much else
    - new players can join until a quorum is reached
  - collect-gestures
    - "
  (:require [spellcast.phase.ingame]
            [spellcast.phase.pregame]
            [spellcast.phase.postgame]))
