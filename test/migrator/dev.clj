(ns migrator.dev
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  ;; (:use [cljs.repl :only [repl]]
  ;;       [cljs.repl.browser :only [repl-env]])
  (:require [clojure.java.io :as io]
            [clojure.java.javadoc :refer [javadoc]]
            [clojure.pprint :refer [pprint]]
            [clojure.reflect :refer [reflect]]
            [clojure.repl :refer [apropos dir doc find-doc pst source]]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :as test]
            ;; [clojure.core.async :refer [>!! <!! >! <! go-loop alt! timeout]]
            [clojure.tools.namespace.repl :as tn]
            [mount.core :as mount]
            ))  ;; <<<< replace this your "app" namespace(s) you want to be available at REPL time

(defn start []
  (mount/start))

(defn stop []
  (mount/stop))

(defn refresh []
  (stop)
  (tn/refresh))

(defn refresh-all []
  (stop)
  (tn/refresh-all))

(defn go
  "starts all states defined by defstate"
  []
  (start)
  :ready)

(defn reset
  "stops all states defined by defstate, reloads modified source files, and restarts the states"
  []
  (stop)
  (tn/refresh :after 'dev/go))


(comment
  (reset)

  )
