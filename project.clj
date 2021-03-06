(defproject spellcast "0.1.0-SNAPSHOT"
  :description "A Clojure implementation of the Bartle/Plotkin classic"
  :url "https://github.com/toxicfrog/spellcast"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/algo.generic "0.1.2"]
                 [org.clojure/core.async "0.1.301.0-deb34a-alpha"]
                 [com.taoensso/timbre "3.1.6"]]
  :main ^:skip-aot spellcast.core
  ; get full stack traces on error
  :repl-options {:init (require '[taoensso.timbre :as log])
                 :caught log/error}
  ; handle multiple UIs by defining a different profile for each one, each
  ; with its own :main and :uberjar-name, and then
  ; $ lein with-profile tty,gui,http uberjar
  :profiles
  {:uberjar {:aot :all}})
