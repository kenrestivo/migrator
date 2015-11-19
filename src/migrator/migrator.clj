(ns migrator.migrator
  (:require [cheshire.core :as json]
            [clojure.data :as data]
            [utilza.file :as ufile]
            [mount :as mount]
            [migrator.net :as net]
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




(def paths
  {:users "%s/migrator/export/users"
   :channels "%s/migrator/export/channel_hashes/%s"
   :identity "%s/migrator/export/identity/%s"
   :items  "%s/migrator/export/items/%s/%s/%s"
   :first-post "%s/migrator/export/first_post/%s"
   :version "%s/migrator/version"})


(s/defn save-wrap
  [fetch :- utils/Serv
   save-path :- s/Str
   path-type :- s/Keyword
   & args]
  (when-let [data  (->>  (apply utils/pathify (concat [fetch paths path-type] args))
                         (net/fetcher fetch))]
    (log/trace "making dir for" save-path)
    (jio/make-parents save-path)
    (log/trace "saving" path-type args)
    (spit save-path data)))



(s/defn get-channels
  [{:keys [fetch storage]} :- net/FetchArgs]
  (doseq [{:keys [account_id]} (utils/slurp-accounts storage)
          :let [{:keys [save-directory]} storage
                dir (umisc/inter-str "/" [save-directory account_id])
                aid (Integer/parseInt account_id)]]
    (log/debug "getting channels for" aid)
    ;; TODO: check the table
    (utils/catcher
     (save-wrap fetch (umisc/inter-str "/" [dir utils/channels-file]) :channels aid))))



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
  [settings :- utils/Serv
   channel-hash :- s/Str]
  (log/debug "Checking first post date for" channel-hash)
  ;; TODO: 404 is totally OK here, trap that, don't log it as error, it's not really exceptional
  (some-> settings
          (net/fetcher (utils/pathify settings paths :first-post channel-hash))
          (json/decode true)
          :date
          split-year-month))


(s/defn walk-dir-channel
  [save-directory]
  (for [d (utils/directory-names save-directory)
        f (ufile/file-names (umisc/inter-str "/" [save-directory  d]) (re-pattern utils/channels-file))
        c (channels-from-json (umisc/inter-str "/" [save-directory d f]))]
    {:dir d
     :channel c}))


(s/defn get-identities
  "This is the core of the fetcher"
  [{:keys [fetch storage] :as settings} :- net/FetchArgs]
  (let [{:keys [save-directory]} storage]
    (doseq [{:keys [dir channel]} (walk-dir-channel save-directory)
            :let [identity-path (umisc/inter-str "/" [save-directory dir channel utils/identity-file])]]
      (log/info "Getting identity for account" dir ": " channel)
      (try
        (save-wrap fetch identity-path :identity channel)
        (catch Exception e
          (log/error e))))))


(s/defn get-channel-items
  [{:keys [fetch storage] :as settings} :- net/FetchArgs
   acct-dir :- s/Str
   channel-hash :- s/Str]
  (let [{:keys [year month]} (get-first-ym fetch channel-hash)
        {:keys [save-directory]} storage]
    (when (and year month) ;; don't fetch if there's nohody home
      (log/info "Getting channel items for " channel-hash "starting" year month)
      (doseq [{:keys [year month]} (utils/intervening-year-months year month)
              :let [item-path (umisc/inter-str "/" 
                                               [save-directory acct-dir channel-hash 
                                                year month "items.json"])]]
        (try
          (save-wrap fetch item-path :items channel-hash year month)
          (catch Exception e
            (log/error e)))))))


(s/defn get-items
  [{:keys [fetch storage] :as settings} :- net/FetchArgs]
  (doseq [{:keys [dir channel]} (walk-dir-channel (:save-directory storage))]
    (log/info "Getting items for" dir channel)
    (try 
      (get-channel-items settings dir channel)
      (catch Exception e
        (log/error e)))))



(s/defn get-accounts
  [{:keys [fetch storage] :as settings} :- net/FetchArgs]
  (let [{:keys [save-directory]} storage]
    (save-wrap fetch (umisc/inter-str "/" [save-directory utils/accounts-file]) :users)))

(s/defn run-fetch
  [{:keys [fetch storage] :as settings} :- net/FetchArgs]
  (try
    (net/test-version fetch)
    (doto settings
      get-accounts
      get-channels
      get-identities
      get-items)
    (log/info "Completed run for" settings)
    (catch Exception e
      (log/error e "Run failed to complete"))))



(s/defn setup-fetch
  [{:keys [storage] :as settings} :- net/FetchArgs]
  (log/info "Setting up fetcher")
  (try
    ;; just utils/accounts-file instead?
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
    (future (try 
              (s/with-fn-validation
                (run-fetch fetch))
              (catch Exception e
                (log/error e)))))

  
  (future-done? running)

  (future-cancel running)


  )

