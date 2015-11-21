(ns migrator.dev
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require [clojure.tools.namespace.repl :as tn]
            [mount.core :as mount]))  ;; <<<< replace this your "app" namespace(s) you want to be available at REPL time

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
