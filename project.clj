(defproject spellcast "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [lib-noir "0.4.6"]]
  :plugins [[lein-ring "0.8.2"]]
  :ring {:handler spellcast.handler/app}
  :main spellcast.core
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]]}})
