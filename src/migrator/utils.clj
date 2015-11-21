(ns migrator.utils
  (:require [schema.core :as s]
            [taoensso.timbre :as log]
            [robert.bruce :as bruce]
            [utilza.misc :as umisc]
            [hiccup.util :as h]
            [utilza.file :as ufile]
            [utilza.json :as ujson]
            [clojure.java.io :as jio])
  (:import java.io.File
           org.joda.time.DateTime))


;; TODO: move to conf file? somewhere?
(def accounts-file "accounts.json")
(def channels-file "channels.json")
(def identity-file  "identity.json")
(def items-file  "items.json")


(def min-supported-versions {:hubzilla 1223
                             :redmatrix 1222
                             :plugin 5})



;;; TODO: move to utilza
(defn directory-names
  "Returns seq of subdirectories"
  [directory-name]
  (for [f (->> directory-name File. .listFiles)
        :when (->> f .isDirectory  boolean)]
    (.getName f)))




(def ServerCoords {(s/required-key :login) s/Str
                   (s/required-key :pw) s/Str
                   (s/required-key :base-url) s/Str})


(def Serv
  (merge ServerCoords
         {(s/required-key :max-retries) s/Int
          (s/required-key :socket-timeout) s/Int
          (s/optional-key :seize) s/Bool ;; only relevant for push, but i can't  maintain two maps
          (s/required-key :conn-timeout) s/Int
          (s/required-key :retry-wait) s/Int}))



(def Storage
  {(s/required-key :save-directory) s/Str})


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


(defn make-retry-fn
  "Retries, with backoff. Logs non-fatal errors as wern, fatal as error"
  [retry-wait max-retries]
  (fn retry
    [ex try-count http-context]
    (log/warn ex http-context)
    (Thread/sleep (* try-count retry-wait))
    ;; TODO: might want to try smaller chunks too!
    (if (> try-count max-retries) 
      false
      ;; TODO: save the error status in the database too. do it here, or throw it
      (log/error ex try-count http-context))))


(defn trust-settings
  []
  {:trust-store (->  "cacerts.jks" jio/resource .toString)
   :trust-store-type "jks" 
   :trust-store-pass "none"})


(defmacro catcher 
  [body]
  `(try
     ~body
     (catch Exception e#
       (log/error e#))))



(s/defn pathify
  [{:keys [base-url] :as settings} :- Serv 
   paths :- {s/Keyword s/Str}
   path-type :- s/Keyword
   & args]
  (let [munged-args (concat [(paths path-type) base-url] (map h/url-encode args))]
    (doseq [a munged-args] ;;; XXX hack
      (log/trace a))
    (apply format munged-args)))



(s/defn slurp-accounts
  [storage :- Storage]
  (-> storage 
      :save-directory 
      (str "/" accounts-file) 
      ujson/slurp-json 
      :users))


(s/defn clean-platform
  [platform :- s/Str]
  (->> platform 
       (re-matches #".+?\.(\d+).*?$" )
       second
       Integer/parseInt))


(defn channels-from-json
  [filename]
  (log/trace "Reading channels file for channels" filename)
  (->> filename
       ujson/slurp-json
       :channel_hashes
       (map :channel_hash)))



(s/defn walk-dir-channel
  [save-directory]
  (for [d (directory-names save-directory)
        f (ufile/file-names (umisc/inter-str "/" [save-directory  d]) (re-pattern channels-file))
        c (channels-from-json (umisc/inter-str "/" [save-directory d f]))]
    {:dir d
     :channel c}))




(s/defn bruceify
  [{:keys [max-retries retry-wait]} :- Serv]
  {:decay :double 
   :tries max-retries 
   :sleep retry-wait
   :error-hook #(log/debug (.getMessage %))})


(defmacro bruce-wrap
  [options body]
  `(try
     (bruce/try-try-again ~options
                          (fn [] 
                            ~body))
     (catch Exception e#
       (log/error e#))))


(defn un-404
  [e] 
  (log/debug (.getMessage e))
  (if (or (not (instance? clojure.lang.ExceptionInfo e))  ;; it's not even a clj-http error
          (some-> e .data :status #{404 401}))
    false ;; don't retry
    nil)) ;; retry as normal


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment



  )
