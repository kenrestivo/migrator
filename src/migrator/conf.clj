(ns migrator.conf
  (:require  [schema.core :as s]
             [clojure.edn :as edn]
             [migrator.mmemdb :as memdb]
             [migrator.migrator :as m]
             [mount :as mount]
             [migrator.log :as mlog]
             [taoensso.timbre :as log]))


(defonce conf-file (atom "")) ;; XXX hack until mount supports args

(def Memdb
  {(s/required-key :filename) s/Str
   (s/required-key :autosave-timeout) s/Int})



(def Settings
  {(s/required-key :fetch) m/Fetch
   (s/required-key :storage) m/Storage
   (s/required-key :db) memdb/Memdb
   (s/required-key :log) mlog/Log
   })


(defn read-and-validate
  [conf-file]
  (println "Loading conf file" conf-file)
  (->> conf-file
       slurp
       edn/read-string
       (s/validate Settings)))


