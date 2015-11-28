(ns migrator.core
  (:require [migrator.conf :as conf]
            [migrator.migrator :as m]
            [migrator.push :as p]
            [migrator.net :as net]
            [mount.core :as mount]
            [schema.core :as s]
            [mount.core :as mount]
            [taoensso.timbre :as log])
  (:gen-class))


(defn run-all
  []
  (when (=  (-> p/push :push :base-url)
            (-> m/fetch :fetch :base-url))
    ;; HACK. TODO: move this to the config file checking
    (throw (Exception. "Wrong. You can't migrate to the same server you're migrating from.")))

  ;; don't wait for a full fetch before yelling if push server is misconfigured
  (when (-> p/push :push net/test-version)
    (m/run-fetch m/fetch)  ;; will check fetch version before run
    (p/run-push p/push)
    (log/info "Migration complete! Check for errors.")))


(defn -main
  [& [conf-file-arg & _]]
  (try
    (let [conf-file (or conf-file-arg "config.edn")]
      (println "Starting components" conf-file)
      (mount/start-with-args (conf/read-and-validate conf-file)))
    (run-all)
    (System/exit 0)
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
                              "/home/cust/spaz/src/migrator-configs/spazhub-test.yml")))
    )



  (do 
    (mount/stop)
    (s/with-fn-validation
      (mount/start-with-args (conf/read-and-validate
                              "/home/cust/spaz/src/migrator-configs/redmatrix-test.yml")))
    )

  (conf/read-and-validate "config.edn")

  
  (def running
    (future (try 
              (s/with-fn-validation
                (run-all))
              (catch Exception e
                (log/error e)))))

  
  (future-done? running)

  (future-cancel running)
  


  )
