(defproject spellcast "0.1.0-SNAPSHOT"
  :description "A Clojure implementation of the Bartle/Plotkin classic"
  :url "https://github.com/toxicfrog/spellcast"
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :main ^:skip-aot spellcast.core
  ; handle multiple UIs by defining a different profile for each one, each
  ; with its own :main and :uberjar-name, and then
  ; $ lein with-profile tty,gui,http uberjar
  :profiles
  {:uberjar {:aot :all}})
