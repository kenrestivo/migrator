(ns migrator.net
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

(def paths
  {:version "%s/migrator/version"})




(def FetchArgs
  {(s/required-key :fetch) utils/Serv
   (s/required-key :storage) utils/Storage})

(s/defn fetcher
  "GETs URL with basic auth"
  [{:keys [retry-wait max-retries login pw]} :- utils/Serv
   url :- s/Str]
  (log/trace "fetcher: fetching" url)
  (-> url
      (client/get (merge {:basic-auth [login pw]
                          :throw-entire-message? true
                          :retry-handler (utils/make-retry-fn retry-wait max-retries)
                          :query-params {}} 
                         (utils/trust-settings)))
      :body))




(s/defn test-version
  [{:keys [base-url] :as fetch} :- utils/Serv]
  ;; TODO: check for incorrect plugin path, maybe by testing the WRONG path to see if it succeeds?
  (log/info "Testing to make sure your migrator plugin version is supported")
  (let [{:keys [platform platform_version migrator_version] :as v} 
        (utils/catcher 
         (-> fetch
             (fetcher 
              (utils/pathify fetch paths :version))
             (json/decode true)))
        {:keys [hubzilla redmatrix plugin]} utils/min-supported-versions
        cleaned-platform (utils/clean-platform platform_version)]
    (log/trace "test-version got" v)
    (when (> plugin migrator_version)
      (throw (Exception. 
              (format
               "Server on %s has migrator plugin version %d which is too old. Upgrade it to %d or higher."
               base-url
               migrator_version
               plugin))))
    (case platform
      "hubzilla" (when (< cleaned-platform hubzilla)
                   (throw (Exception. 
                           (format
                            "Server on %s has hubzilla version %d which is too old. Upgrade it to %d or higher."
                            base-url
                            platform_version
                            hubzilla))))
      "redmatrix" (when (< cleaned-platform redmatrix)
                    (throw (Exception. 
                            (format
                             "Server on %s has redmatrix version %d which is too old. Upgrade it to %d or higher."
                             base-url
                             platform_version
                             redmatrix))))
      (throw (Exception. 
              (format
               "Unsupported platform: %s on %s"
               platform
               base-url))))
    v))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment




  )
