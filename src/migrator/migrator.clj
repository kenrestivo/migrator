(ns migrator.migrator
  (:require [cheshire.core :as json]
            [clojure.data :as data]
            [utilza.file :as ufile]
            [mount :as mount]
            [utilza.misc :as umisc]
            [utilza.json :as ujson]
            [clojure.java.io :as jio]
            [schema.core :as s]
            [migrator.mmemdb :as memdb]
            [taoensso.nippy :as nippy]
            [clj-http.client :as client]
            [migrator.utils :as utils]
            [clojure.java.io :as jio]
            [taoensso.timbre :as log]
            [utilza.repl :as urepl]))

(def accounts-file "accounts.json")
(def channels-file "channels.json")
(def min-supported-plugin-version 1)

(def ServerCoords {(s/required-key :login) s/Str
                   (s/required-key :pw) s/Str
                   (s/required-key :base-url) s/Str})


(def Pool 
  {(s/required-key :timeout) s/Int
   (s/required-key :threads) s/Int
   (s/required-key :insecure?) s/Bool
   (s/required-key :default-per-route) s/Int})


(def Fetch
  (merge ServerCoords
         {(s/required-key :max-retries) s/Int
          (s/required-key :retry-wait) s/Int
          (s/required-key :pool) Pool}))

(def Storage
  {(s/required-key :save-directory) s/Str})

(def FetchArgs
  {(s/required-key :fetch) Fetch
   (s/required-key :storage) Storage})


(def paths
  {:users "%s/migrator/export/users"
   :channels "%s/migrator/export/channel_hashes/%d"
   :identities "%s/migrator/export/identity/%s"
   :version "%s/migrator/version"})



(s/defn pool-settings
  [{:keys [pool]} :- Fetch]
  (merge (:pool pool
                {:trust-store (->  "cacerts.jks" jio/resource .toString)
                 :trust-store-type "jks" 
                 :trust-store-pass "none"})))


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



(defmacro catcher 
  [body]
  `(try
     ~body
     (catch Exception e#
       (log/error e#))))




(s/defn fetcher
  "GETs URL with basic auth"
  [url :- s/Str
   {:keys [retry-wait max-retries login pw]} :- Fetch]
  (log/trace "fetcher: fetching" url)
  (catcher
   (-> url
       (client/get {:basic-auth [login pw]
                    :throw-entire-message? true
                    :as :stream
                    :retry-handler (make-retry-fn retry-wait max-retries)
                    :query-params {}})
       :body)))



(s/defn fetch-users
  [{:keys [base-url] :as settings} :- Fetch]
  (log/trace "fetching users" base-url)
  (some-> paths
          :users
          (format  base-url)
          (fetcher settings)))


(s/defn fetch-channels
  [{:keys [base-url] :as settings} :- Fetch
   account-id :- s/Int]
  {:pre [(-> account-id nil? not)]}
  (log/trace "fetching channels for " base-url account-id)
  (some-> paths
          :channels
          (format base-url account-id)
          (fetcher settings)))



(s/defn fetch-identity
  [{:keys [base-url] :as settings} :- Fetch
   channel-hash :- s/Str]
  (log/trace "fetching identity (channel) for " base-url channel-hash)
  (some-> paths
          :identities
          (format  base-url channel-hash)
          (fetcher settings)))

(s/defn fetch-version
  [{:keys [base-url] :as settings} :- Fetch]
  (log/trace "fetching version" base-url)
  (some-> paths
          :version
          (format  base-url)
          (fetcher settings)))


(s/defn save-identity
  [{:keys [fetch storage]} :- FetchArgs
   channel-hash :- s/Str
   path :- s/Str]
  (log/trace "saving identity" channel-hash path)
  (some->  fetch
           (fetch-identity channel-hash)
           (jio/copy (java.io.File. path))))

(s/defn save-accounts
  [{:keys [fetch storage]} :- FetchArgs
   path :- s/Str]
  (some->  fetch
           fetch-users
           (jio/copy (java.io.File. path))))


(s/defn get-channels
  [{:keys [fetch storage]} :- FetchArgs]
  (doseq [{:keys [account_id]} (-> storage 
                                   :save-directory 
                                   (str "/" accounts-file) 
                                   ujson/slurp-json 
                                   :users)
          :let [dir (-> storage :save-directory (str "/" account_id))
                aid (Integer/parseInt account_id)]]
    ;; TODO: error checking, restart
    (log/info "checking/making dir for" dir)
    (jio/make-parents (str dir "/.start"))
    (some-> fetch
            (fetch-channels aid)
            (jio/copy (java.io.File. (str dir "/" channels-file)))
            )))

(defn channels-from-json
  [filename]
  (log/trace "Reading channels file for channels" filename)
  (->> filename
       ujson/slurp-json
       :channel_hashes
       (map :channel_hash)))


(s/defn get-identities
  "This is the core of the fetcher"
  [{:keys [fetch storage] :as settings} :- FetchArgs]
  (let [{:keys [save-directory]} storage]
    (doseq [d (utils/directory-names save-directory)]
      (doseq [f (ufile/file-names (umisc/inter-str "/" [save-directory  d]) (re-pattern channels-file))]
        (doseq [c (channels-from-json (umisc/inter-str "/" [save-directory d f]))]
          (log/trace "getting identity" d f c)
          (try
            ;; TODO: check that they don't already exist? and aren't errored?
            (save-identity settings c (umisc/inter-str "/" [save-directory d (str c ".json")]))
            (catch Exception e
              (log/error e)))
          )))))


(s/defn test-version*
  [settings :- Fetch]
  (-> settings
      fetch-version
      slurp
      (json/decode true)
      :version))

(s/defn test-version
  [{:keys [fetch] :as settings} :- FetchArgs]
  (try
    (let [v (test-version* fetch)]
      (<= min-supported-plugin-version v))
    (catch Exception e
      ;; TODO: check for incorrect plugin path, maybe by testing the WRONG path to see if it succeeds?
      (log/error e))))


(s/defn run-fetch
  [{:keys [fetch storage] :as settings} :- FetchArgs]
  (try
    (client/with-connection-pool (pool-settings fetch)
      (when (test-version settings)
        (doto settings
          (save-accounts (str (:save-directory storage) "/" accounts-file))
          get-channels
          get-identities)))
    (log/info "Completed run for" settings)
    (catch Exception e
      (log/error e))))



(s/defn setup-fetch
  [{:keys [storage] :as settings} :- FetchArgs]
  (log/info "Setting up fetcher")
  (try
    ;; just accounts-file instead?
    (log/info "Making top-level save directory")
    (-> storage :save-directory (str "/.start") jio/make-parents)
    (catch Exception e
      (log/error e)))
  settings)



(defn stop-fetch
  []
  {})

(mount/defstate fetch
  :start (setup-fetch (select-keys (mount/args) [:fetch :storage]))
  :stop (stop-fetch))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

  (log/set-level! :trace)

  (log/set-level! :info)

  (log/info "wtf")


  (s/with-fn-validation
    (mount/start))

  (mount/stop)

  (s/explain FetchArgs)
  
  (s/with-fn-validation
    (run-fetch fetch))

  (s/with-fn-validation
    (test-version fetch))
  


  )

