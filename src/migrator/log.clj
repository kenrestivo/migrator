(ns migrator.log
  (:require [clojure.tools.trace :as trace]
            [migrator.utils :as utils]
            [mount.core :as mount]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [utilza.java :as ujava]))




(def Log
  {(s/required-key :spit-filename) s/Str
   :level s/Keyword})

(defn start-logger
  [{:keys [log] :as config}]
  (let [{:keys [spit-filename]} log]
    (println "starting logging")
    (log/merge-config! (merge log
                              {:output-fn (partial log/default-output-fn {:stacktrace-fonts {}})
                               :appenders {:println (appenders/println-appender {:enabled? false})
                                           :spit (appenders/spit-appender
                                                  {:fname spit-filename})}}))
    ;; TODO: only in dev envs4
    (alter-var-root #'clojure.tools.trace/tracer (fn [_]
                                                   (fn [name value]
                                                     (log/debug name value))))
    (log/info "Welcome to Migrator" (ujava/revision-info "migrator" "migrator"))
    (log/info "logging started" (utils/redact config))
    log)) ;; so it gets into the state



(mount/defstate log 
  :start (start-logger (mount/args))
  :stop (log/info "Shutting down logger"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

  (log/error (Exception. "foobar"))
  (println (.getCause *e))






  )
