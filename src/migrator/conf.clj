(ns migrator.conf
  (:require  [schema.core :as s]
             [clojure.edn :as edn]
             [migrator.migrator :as m]
             [migrator.utils :as utils]
             [mount :as mount]
             [clojure.java.io :as jio]
             [migrator.log :as mlog]
             [taoensso.timbre :as log]))



(def Memdb
  {(s/required-key :filename) s/Str
   (s/required-key :autosave-timeout) s/Int})



(def Settings
  {(s/required-key :fetch) utils/Serv
   (s/required-key :push) utils/Serv
   (s/required-key :storage) utils/Storage
   ;;; (s/required-key :db) Memdb not needed yet
   (s/required-key :log) mlog/Log
   })



(defn read-and-validate
  [conf-file]
  (println "Loading conf file" conf-file)
  (let [defaults (-> "defaults/standard-defaults.edn" jio/resource slurp edn/read-string)]
    (->> conf-file
         slurp
         edn/read-string
         (merge-with merge defaults)
         (s/validate Settings))))


