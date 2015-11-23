(defproject migrator "0.1.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]
                 [clj-time "0.11.0"]
                 [org.clojure/core.async "0.2.374" :exclusions [org.clojure/tools.reader]]
                 [mount "0.1.3"]
                 [com.taoensso/timbre "4.1.4"]
                 [prismatic/schema "1.0.3"]
                 [utilza "0.1.80"]
                 [org.clojure/tools.trace "0.7.9"]
                 [clj-yaml "0.4.0" :exclusions [org.yaml/snakeyaml]]
                 [org.yaml/snakeyaml "1.16"]
                 [robert/bruce "0.8.0"]
                 [com.taoensso/nippy "2.10.0"]
                 [hiccup "1.0.5"]
                 [clj-http "2.0.0"]
                 ]
  :capsule {:execution {:boot  {:scripts {:unix "" :windows ""}}}
            :runtime {:min-java-version "7" :java-version "8"}
            :types {
                    :fat {} } } 
  :main migrator.core
  :profiles {:repl {:timeout 180000}
             :uberjar {:aot [migrator.core]
                       :uberjar-name "migrator.jar"}
             :dev  {:jvm-opts ["-client"]
                    :plugins [
                              [lein-capsule "0.2.0"] ]}}

  )



