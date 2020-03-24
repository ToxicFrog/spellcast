(defproject ca.ancilla/spellcast "0.0.1"
  :description "WIP UI for spellcast-by-web"
  :url "https://github.com/toxicfrog/spellcast"
  :license {:name "Apache"
            :url  "http://www.apache.org/licenses/"
            :distribution :repo}
  :min-lein-version "2.4.0"
  :global-vars {*warn-on-reflection* true
                *assert* true}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [expound "0.8.4"]
                 [io.aviso/pretty "0.1.37"]
                 [compojure "1.6.1" :exclusions [ring/ring-core]]
                 [hiccup "1.0.5"]
                 [ring/ring-defaults "0.3.2" :exclusions [ring/ring-core]]
                 [ring "1.8.0"]
                 [prismatic/schema "1.1.12"]
                 [caribou/antlers "0.6.1"]
                 ]
  :plugins [[lein-ring "0.12.5"]
            [io.aviso/pretty "0.1.37"]]
  :middleware [io.aviso.lein-pretty/inject]
  :ring {:handler spellcast.routes/handler
         :init spellcast.routes/init
         :destroy spellcast.routes/destroy}
  :profiles
  {:uberjar {:aot :all}
   :production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false :port 8099}}
   :dev
   {:dependencies [[ring/ring-mock "0.4.0"] [ring/ring-devel "1.7.1"]]
    :ring {:open-browser? false :port 8099}}})
