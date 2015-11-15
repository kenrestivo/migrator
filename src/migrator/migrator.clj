(ns migrator.migrator
  (:require [cheshire.core :as json]
            [clojure.data :as data]
            [utilza.file :as ufile]
            [mount :as mount]
            [utilza.misc :as umisc]
            [utilza.json :as ujson]
            [clojure.java.io :as jio]
            [schema.core :as s]
            ;; [utilza.mmemdb :as memdb] ;; not really needed yet
            [taoensso.nippy :as nippy]
            [clj-http.client :as client]
            [migrator.utils :as utils]
            [clojure.java.io :as jio]
            [taoensso.timbre :as log]
            [utilza.repl :as urepl]))

(def accounts-file "accounts.json")
(def channels-file "channels.json")
(def identity-file  "identity.json")

(def min-supported-plugin-version 2)

(def ServerCoords {(s/required-key :login) s/Str
                   (s/required-key :pw) s/Str
                   (s/required-key :base-url) s/Str})


(def Fetch
  (merge ServerCoords
         {(s/required-key :max-retries) s/Int
          (s/required-key :retry-wait) s/Int}))

(def Storage
  {(s/required-key :save-directory) s/Str})

(def FetchArgs
  {(s/required-key :fetch) Fetch
   (s/required-key :storage) Storage})


(def paths
  {:users "%s/migrator/export/users"
   :channels "%s/migrator/export/channel_hashes/%d"
   :identity "%s/migrator/export/identity/%s"
   :items  "%s/migrator/export/items/%s/%d/%d"
   :first-post "%s/migrator/export/first_post/%s"
   :version "%s/migrator/version"})


(defn trust-settings
  []
  {:trust-store (->  "cacerts.jks" jio/resource .toString)
   :trust-store-type "jks" 
   :trust-store-pass "none"})



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
       (client/get (merge {:basic-auth [login pw]
                           :throw-entire-message? true
                           :retry-handler (make-retry-fn retry-wait max-retries)
                           :query-params {}} 
                          (trust-settings)))
       :body)))



(s/defn fetch-wrap
  [{:keys [base-url] :as settings} :- Fetch 
   path-type :- s/Keyword
   & args]
  (log/debug "fetching" base-url path-type args)
  (some-> (apply format (concat [(paths path-type) base-url] args))
          (fetcher settings)))



(s/defn save-wrap
  [fetch :- Fetch
   save-path :- s/Str
   path-type :- s/Keyword
   & args]
  (log/trace "making dir for" save-path)
  (jio/make-parents save-path)
  (log/trace "saving" path-type args)
  (->> args
       (cons path-type)
       (cons fetch)
       (apply fetch-wrap)
       (spit save-path)))



(s/defn get-channels
  [{:keys [fetch storage]} :- FetchArgs]
  (doseq [{:keys [account_id]} (-> storage 
                                   :save-directory 
                                   (str "/" accounts-file) 
                                   ujson/slurp-json 
                                   :users)
          :let [{:keys [save-directory]} storage
                dir (umisc/inter-str "/" [save-directory account_id])
                aid (Integer/parseInt account_id)]]
    ;; TODO: error checking, restart
    (catcher
     (save-wrap fetch (umisc/inter-str "/" [dir channels-file]) :channels aid))))



(defn channels-from-json
  [filename]
  (log/trace "Reading channels file for channels" filename)
  (->> filename
       ujson/slurp-json
       :channel_hashes
       (map :channel_hash)))




(defn split-year-month
  [s]
  (->> (clojure.string/split s  #"-")
       (take 2)
       (map #(Integer/parseInt %))
       (zipmap [:year :month])))


(s/defn get-first-ym
  [settings :- Fetch
   channel-hash :- s/Str]
  (log/debug "Checking first post date for" channel-hash)
  ;; TODO: 404 is totally OK here, trap that, don't log it as error, it's not really exceptional
  (some-> settings
          (fetch-wrap :first-post channel-hash)
          (json/decode true)
          :date
          split-year-month))


(s/defn walk-dir-channel
  [save-directory]
  (for [d (utils/directory-names save-directory)
        f (ufile/file-names (umisc/inter-str "/" [save-directory  d]) (re-pattern channels-file))
        c (channels-from-json (umisc/inter-str "/" [save-directory d f]))]
    {:dir d
     :channel c}))


(s/defn get-identities
  "This is the core of the fetcher"
  [{:keys [fetch storage] :as settings} :- FetchArgs]
  (let [{:keys [save-directory]} storage]
    (doseq [{:keys [dir channel]} (walk-dir-channel save-directory)
            :let [identity-path (umisc/inter-str "/" [save-directory dir channel identity-file])]]
      (log/info "Getting identity for account" dir ": " channel)
      (try
        (save-wrap fetch identity-path :identity channel)
        (catch Exception e
          (log/error e))))))


(s/defn get-channel-items
  [{:keys [fetch storage] :as settings} :- FetchArgs
   acct-dir :- s/Str
   channel-hash :- s/Str]
  (let [{:keys [year month]} (get-first-ym fetch channel-hash)
        {:keys [save-directory]} storage]
    (when (and year month) ;; don't fetch if there's nohody home
      (doseq [{:keys [year month]} (utils/intervening-year-months year month)
              :let [item-path (umisc/inter-str "/" 
                                               [save-directory acct-dir channel-hash 
                                                year month "items.json"])]]
        (try
          (save-wrap fetch item-path :items channel-hash year month)
          (catch Exception e
            (log/error e)))))))


(s/defn get-items
  [{:keys [fetch storage] :as settings} :- FetchArgs]
  (doseq [{:keys [dir channel]} (walk-dir-channel (:save-directory storage))]
    (log/info "Getting items for" dir channel)
    (try 
      (get-channel-items settings dir channel)
      (catch Exception e
        (log/error e)))))


(s/defn test-version*
  [settings :- Fetch]
  (-> (fetch-wrap settings :version)
      (json/decode true)
      :version))

(s/defn test-version
  [{:keys [fetch] :as settings} :- FetchArgs]
  (log/info "Testing to make sure your migrator plugin version is supported")
  (try
    (let [v (test-version* fetch)]
      (<= min-supported-plugin-version v))
    (catch Exception e
      ;; TODO: check for incorrect plugin path, maybe by testing the WRONG path to see if it succeeds?
      (log/error e))))

(s/defn get-account
  [{:keys [fetch storage] :as settings} :- FetchArgs]
  (let [{:keys [save-directory]} storage]
    (save-wrap fetch (umisc/inter-str "/" [save-directory accounts-file]) :users)))

(s/defn run-fetch
  [{:keys [fetch storage] :as settings} :- FetchArgs]
  (try
    (when (test-version settings)
      (doto settings
        get-account
        get-channels
        get-identities
        get-items))
    (log/info "Completed run for" settings)
    (catch Exception e
      (log/error e "Run failed to complete"))))



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

  (do
    (mount/stop)

    (s/with-fn-validation
      (mount/start))
    )


  
  (def running
    (future (s/with-fn-validation
              (run-fetch fetch))))

  
  (future-done? running)



  )

