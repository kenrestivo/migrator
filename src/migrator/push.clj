(ns migrator.push
  (:require [cheshire.core :as json]
            [clojure.data :as data]
            [schema.core :as s]
            [migrator.net :as net]
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



(def paths
  {:account "%s/migrator/import/account"
   :identity "%s/migrator/import/identity/%s"
   :items  "%s/migrator/import/items/%s/%d/%d"
   :version "%s/migrator/version"})


(def PushArgs
  {(s/required-key :push) utils/Serv
   (s/required-key :storage) utils/Storage})

(s/defn pusher
  "POSTs URL with basic auth"
  [{:keys [retry-wait max-retries login pw socket-timeout conn-timeout]} :- utils/Serv
   url :- s/Str
   account :- {s/Keyword s/Any}]
  (log/trace "pusher: pushing" url)
  (utils/catcher
   (-> url
       (client/post (merge {:basic-auth [login pw]
                            :throw-entire-message? true
                            :headers {"Content-Type" "application/json"}
                            :retry-handler (utils/make-retry-fn retry-wait max-retries)
                            :socket-timeout socket-timeout
                            :conn-timeout conn-timeout
                            :body (json/encode account)
                            :as :json}
                           (utils/trust-settings)))
       :body)))


;; XXX duplicate/boilerplate, TODO combine with pusher
(s/defn push-file
  "POSTs URL with basic auth"
  [{:keys [retry-wait max-retries login pw socket-timeout conn-timeout]} :- utils/Serv
   url :- s/Str
   email :- s/Str
   filepath :- s/Str]
  (log/trace "pusher: pushing multipart" url)
  (utils/catcher
   (-> url
       (client/post (merge {:basic-auth [login pw]
                            :throw-entire-message? true
                            :socket-timeout socket-timeout
                            :conn-timeout conn-timeout
                            :retry-handler (utils/make-retry-fn retry-wait max-retries)
                            :multipart [{:name "Content/type" :content "application/json"}
                                        {:name "Content-Transfer-Encoding" :content "binary"}
                                        {:name "filename" :content (clojure.java.io/file filepath)}]
                            :as :json}
                           (utils/trust-settings)))
       :body)))


(s/defn upload-accounts
  [{:keys [storage push]} :- PushArgs]
  (doseq [account (utils/slurp-accounts storage)]
    (log/info "uploading acccount for" (:account_email account))
    (let [res (pusher push (utils/pathify push paths :account) account)]
      (log/info res))))

(s/defn emailify
  [accounts :- [{s/Keyword s/Any}]]
  (reduce #(assoc %1 (-> %2 :account_id Integer/parseInt) (:account_email %2)) 
          {} accounts))


(s/defn upload-channels
  [{:keys [storage push]} :- PushArgs]
  (let [{:keys [save-directory]} storage
        accounts (-> storage  utils/slurp-accounts emailify)]
    (doseq [{:keys [dir channel]} (utils/walk-dir-channel (:save-directory storage))
            :let [email (get accounts (Integer/parseInt dir))
                  identity-path (umisc/inter-str "/" [save-directory dir channel utils/identity-file])]]
      (log/info "Uploading channel for" email dir channel)
      (log/trace "from" identity-path)
      (utils/catcher
       (push-file push (utils/pathify push paths :identity email) email identity-path)))))



(s/defn upload-items
  [{:keys [storage push]} :- PushArgs]
  (let [{:keys [save-directory]} storage
        accounts (-> storage  utils/slurp-accounts emailify)]
    (doseq [account-dir (utils/directory-names save-directory)
            channel-hash (utils/directory-names (umisc/inter-str "/" [save-directory account-dir]))
            year (utils/directory-names (umisc/inter-str "/" [save-directory account-dir channel-hash]))
            month  (utils/directory-names (umisc/inter-str "/" [save-directory account-dir channel-hash year]))
            :let [email (get accounts (Integer/parseInt account-dir))
                  f (umisc/inter-str "/" 
                                     [save-directory account-dir channel-hash 
                                      year month utils/items-file])]]
      (log/info "Uploading items for" email channel-hash year month)
      (log/trace email f)
      (utils/catcher
       (push-file push (utils/pathify push paths :identity email) email f))
      )))



(s/defn run-push
  [{:keys [push storage] :as settings} :- PushArgs]
  (try
    (net/test-version push)
    (doto settings
      upload-accounts
      upload-channels
      upload-items
      )
    (log/info "Completed run for" settings)
    (catch Exception e
      (log/error e "Run failed to complete"))))



(s/defn setup-push
  [{:keys [storage] :as settings} :- PushArgs]
  (log/info "Setting up pusher")
  (try
    (log/info "Checking top-level save directory")
    ;; TODO: XXX must make sure there's something there!!
    (catch Exception e
      (log/error e)))
  settings)



(defn stop-push
  []
  {})




(mount/defstate push
  :start (setup-push (select-keys (mount/args) [:push :storage]))
  :stop (stop-push))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment

  
  (def running
    (future (try 
              (s/with-fn-validation
                (run-push push))
              (catch Exception e
                (log/error e)))))

  
  (future-done? running)

  (future-cancel running)



  (def running 
    (try 
      (s/with-fn-validation
        (upload-items push))
      (catch Exception e
        (log/error e))))



  (require '[utilza.repl :as urepl])

  (->> (str (-> push :storage :save-directory) "/" utils/accounts-file)
       ujson/slurp-json
       (urepl/massive-spew  "/tmp/foo.edn"))

  
  )