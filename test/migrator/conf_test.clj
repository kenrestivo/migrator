(ns migrator.conf-test
  (:require [clojure.test :refer :all]
            [migrator.conf :refer :all]
            [utilza.file :as file]))


(defn ymls-with-path
  [path]
  (for [f (file/file-names path  #".*?\.yml$")]
    (str path "/" f)))

(deftest example-confs
  (testing "example configs")
  ;; TODO: need java resoruce path
  (is (every? map?  (for [f (ymls-with-path "resources/config")]
                      (do
                        (testing f)
                        (read-and-validate f))))))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment

  ;;; for dev and production purposes only, not automated
  (deftest dev-confs
    (is (every? map?  (for [f (ymls-with-path "/home/cust/spaz/src/migrator-configs")]
                        (do 
                          (println f)
                          (testing f)
                          (read-and-validate f))))))

  (try
    (run-tests)
    (catch Exception e
      (log/error e)))



  )
