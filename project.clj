(defproject spellcast "0.1.0-SNAPSHOT"
  :description "A Clojure implementation of the Bartle/Plotkin classic"
  :url "https://github.com/toxicfrog/spellcast"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.typed "0.2.44"]
                 [org.clojure/core.async "0.1.301.0-deb34a-alpha"]]
  :main ^:skip-aot spellcast.core
  ; get full stack traces on error
  :repl-options {:caught pst}
  ; handle multiple UIs by defining a different profile for each one, each
  ; with its own :main and :uberjar-name, and then
  ; $ lein with-profile tty,gui,http uberjar
  :profiles
  {:uberjar {:aot :all}})
