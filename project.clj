(defproject migrator "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]
                 [clj-time "0.11.0"]
                 [org.clojure/core.async "0.2.371"]
                 [mount "0.1.1"]
                 [com.taoensso/timbre "4.1.2"]
                 [prismatic/schema "1.0.3"]
                 [utilza "0.1.80"]
                 [com.taoensso/nippy "2.10.0"]
                 [hiccup "1.0.0"]
                 [php-clj "0.4.1"] ;;; XXX do i need this anymore?
                 [clj-http "2.0.0"]
                 ]
  :main migrator.core
  :uberjar-name "migrator.jar"
  :profiles {:repl {:timeout 180000
                    :injections [(do
                                   (require 'migrator.core)
                                   (migrator.core/-main))]}}
  )
