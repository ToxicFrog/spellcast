(ns spellcast.phase.all
  "An empty namespace that does nothing but pull in all the other phases, to make sure that their event handlers are registered."
  (:require [spellcast.phase.ingame]
            [spellcast.phase.pregame]))
