(ns migrator.core
  (:require [migrator.log :as mlog]
            [migrator.migrator :as m]
            [schema.core :as s]
            [taoensso.timbre :as log]
            ;; [utilza.mmemdb :as memdb] not needed yet
            [mount :as mount]
            [migrator.conf :as conf]
            )
  (:gen-class))




(defn -main
  [& [conf-file-arg & _]]
  (try
    (let [conf-file (or conf-file-arg "config.edn")]
      (println "Starting components" conf-file)
      (mount/start-with-args (conf/read-and-validate conf-file)))
    (catch Exception e
      (println (.getMessage e))
      (println (.getCause e)))))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(comment

  (future (-main "config.edn"))

  (s/with-fn-validation
    (mount/start-with-args (conf/read-and-validate "config.edn")))

  memdb/memdb
  (mount/args)
  m/fetch

  (s/with-fn-validation
    (mount/start))
  
  (mount/stop)

  (log/error (.getCause *e))

  (do 
    (mount/stop)
    (s/with-fn-validation
      (mount/start-with-args (conf/read-and-validate
                              "/home/cust/spaz/src/migrator-configs/spazhub-test.edn")))
    )

  (conf/read-and-validate "config.edn")

  

  )
