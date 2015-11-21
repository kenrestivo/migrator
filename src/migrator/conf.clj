(ns migrator.conf
  (:require  [schema.core :as s]
             [clojure.edn :as edn]
             [migrator.migrator :as m]
             [migrator.utils :as utils]
             [mount.core :as mount]
             [clojure.java.io :as jio]
             [clj-yaml.core :as yaml]
             [schema.coerce :as c]
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
  (let [coercer (c/coercer Settings c/json-coercion-matcher)
        defaults (-> "defaults/standard-defaults.yml" jio/resource slurp yaml/parse-string)]
    (->> conf-file
         slurp
         yaml/parse-string
         (merge-with merge defaults)
         coercer
         (s/validate Settings))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


