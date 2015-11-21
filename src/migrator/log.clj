(ns migrator.log
  (:require [mount.core :as mount]
            [taoensso.timbre :as log]
            [utilza.java :as ujava]
            [schema.core :as s]
            [clojure.tools.trace :as trace]
            [taoensso.timbre.appenders.core :as appenders]))


(def Log
  {(s/required-key :spit-filename) s/Str
   :level s/Keyword})

(defn start-logger
  [{:keys [spit-filename] :as config}]
  (println "starting logging")
  (log/merge-config! (merge config
                            {:output-fn (partial log/default-output-fn {:stacktrace-fonts {}})
                             :appenders {:println (appenders/println-appender {:enabled? false})
                                         :spit (appenders/spit-appender
                                                {:fname spit-filename})}}))
  ;; TODO: only in dev envs4
  (alter-var-root #'clojure.tools.trace/tracer (fn [_]
                                                 (fn [name value]
                                                   (log/debug name value))))
  (log/info "Welcome to Migrator" (ujava/revision-info "migrator" "migrator"))
  (log/info "logging started" config))



(mount/defstate log 
  :start (start-logger (:log (mount/args)))
  :stop (log/info "Shutting down logger"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

  (log/error (Exception. "foobar"))

  ;; XXX HACK UNTIL I GET COMPONENTS INTEGRATED
  (start-logger {:spit-filename "/mnt/sdcard/tmp/web.log4j"})
  
  )
