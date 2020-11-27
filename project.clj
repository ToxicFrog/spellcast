(defproject ca.ancilla/spellcast "0.0.1"
  :description "WIP UI for spellcast-by-web"
  :url "https://github.com/toxicfrog/spellcast"
  :license {:name "Apache"
            :url  "http://www.apache.org/licenses/"
            :distribution :repo}
  :min-lein-version "2.4.0"
  :global-vars {*warn-on-reflection* false
                *assert* true}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.773"]
                 [caribou/antlers "0.6.1"]
                 [com.taoensso/timbre "4.10.0"]
                 [compojure "1.6.1" :exclusions [ring/ring-core]]
                 [expound "0.8.4"]
                 [hiccup "1.0.5"]
                 [io.aviso/pretty "0.1.37"]
                 [prismatic/schema "1.1.12"]
                 [ring "1.8.0"]
                 [ring/ring-defaults "0.3.2" :exclusions [ring/ring-core]]
                 [ring/ring-json "0.5.0" :exclusions [ring/ring-core]]
                 [slingshot "0.12.2"]
                 ; cljs dependencies
                 [org.clojure/core.async "1.3.610"]
                 [cljs-http "0.1.46"]
                 [binaryage/oops "0.7.0"]
                 [reagent "1.0.0-alpha2"]
                 ]
  :plugins [[lein-ring "0.12.5"]
            [io.aviso/pretty "0.1.37"]
            [lein-cljsbuild "1.1.8"]]
  :middleware [io.aviso.lein-pretty/inject]
  :ring {:handler spellcast.core/request-handler
         :init spellcast.core/init
         :destroy spellcast.core/destroy}
  :repl-options {:init-ns spellcast.core}
  :cljsbuild
  {:builds [{:source-paths ["cljs"]
             :jar true
             :notify-command ["notify-send" "-i" "/home/rebecca/opt/cljs.svg"]
             :compiler {:output-to "resources/public/js/main.js"
                        :source-map "resources/public/js/main.js.map"
                        :output-dir "resources/public/js/"
                        :optimizations :whitespace
                        :pretty-print true}}]}
  :profiles
  {:uberjar {:aot :all}
   :production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false :port 8099}}
   :dev
   {:dependencies [[ring/ring-mock "0.4.0"] [ring/ring-devel "1.7.1"]]
    :ring {:open-browser? false :port 8099}}})
