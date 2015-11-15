(ns migrator.json
  (:require [cheshire.core :as json]
            [clojure.data :as data]
            [utilza.json :as ujson]
            [utilza.repl :as urepl])
  )




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment


  (->> "/home/cust/spaz/logs/bamfic-channel.json"
       ujson/slurp-json
       (urepl/massive-spew "/tmp/foo.edn"))


  (->> "/home/cust/spaz/logs/bamfic-content.json"
       ujson/slurp-json
       (urepl/massive-spew "/tmp/foo.edn"))


  
  (->>  ["/home/cust/spaz/logs/bamfic-channel.json"
         "/home/cust/spaz/logs/bamfic-content.json"]
        (map (comp set keys ujson/slurp-json))
        (map #(filter identity %))
        (zipmap [:channel-only :content-only :in-both] ))


  (urepl/massive-spew "/tmp/foo.edn" *1)


  (->> "/tmp/foo.json"
       ujson/slurp-json
       (urepl/massive-spew "/tmp/foo.edn"))


  )
