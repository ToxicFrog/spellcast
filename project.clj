(defproject ca.ancilla/spellcast "0.0.1"
  :description "WIP UI for spellcast-by-web"
  :url "https://github.com/toxicfrog/spellcast"
  :license {:name "Apache"
            :url  "http://www.apache.org/licenses/"
            :distribution :repo}
  :min-lein-version "2.4.0"
  :global-vars {*warn-on-reflection* true
                *assert* true}

  :dependencies
  [[org.clojure/clojure       "1.8.0"]

   [org.clojure/clojurescript "1.8.34"]
   [org.clojure/core.async    "0.2.374"]
   [com.taoensso/sente        "1.8.1"]
   [com.taoensso/timbre       "4.3.1"]

   [http-kit                  "2.2.0-alpha1"]

   [ring                      "1.4.0"]
   [ring/ring-defaults        "0.2.0"]
   [compojure                 "1.5.0"]
   [hiccup                    "1.0.5"]
   ]

  :plugins
  [[lein-pprint         "1.1.2"]
   [lein-ancient        "0.6.8"]
   [lein-cljsbuild      "1.1.3"]
   [lein-ring "0.9.7"]
   ]

  :cljsbuild
  {:builds
   [{:id :cljs-client
     :source-paths ["src"]
     :compiler {:output-to "resources/public/main.js"
                :optimizations :whitespace #_:advanced
                :pretty-print true}}]}

  :main example.server
  :ring {:handler example.server/main-ring-handler}

  ;; Call `lein start-repl` to get a (headless) development repl that you can
  ;; connect to with Cider+emacs or your IDE of choice:
  :aliases
  {"start-repl" ["do" "cljsbuild" "once," "repl" ":headless"]
   "start"      ["do" "cljsbuild" "once," "run"]}

  :repositories
  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
