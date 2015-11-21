(ns migrator.conf
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as jio]
            [migrator.log :as mlog]
            [migrator.utils :as utils]
            [schema.coerce :as c]
            [schema.core :as s]))



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


