(ns migrator.utils
  (:require [schema.core :as s])
  (:import java.io.File
           org.joda.time.DateTime))

;;; TODO: move to utilza
(defn directory-names
  "Returns seq of subdirectories"
  [directory-name]
  (for [f (->> directory-name File. .listFiles)
        :when (->> f .isDirectory  boolean)]
    (.getName f)))


(defn this-year
  []
  (->  (DateTime.)
       (.toString "YYYY")
       Integer/parseInt))

(defn this-month
  []
  (->  (DateTime.)
       (.toString "MM")
       Integer/parseInt))


;; Utiliy function that really should be moved to utilza, or use clj-time"
(s/defn intervening-year-months
  "Returns a seq of maps of years and months in between given year and month, and now"
  [year :- s/Int
   month :- s/Int]
  (for [d (loop [l [(DateTime. year month 01 0 0 0)]]
            (let [m (.plusMonths (last l) 1)]
              (if (.isAfterNow m)
                l
                (recur (conj l m)))))]
    {:year (.getYear d)
     :month (.getMonthOfYear d)}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment



  )
