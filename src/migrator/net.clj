(ns migrator.net
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [migrator.utils :as utils]
            [robert.bruce :as bruce]
            [schema.core :as s]
            [taoensso.timbre :as log]))

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
  (log/info "Testing to make sure your migrator plugin version is supported")
  (let [{:keys [platform platform_version migrator_version] :as v} 
        (try
          (bruce/try-try-again (assoc (utils/bruceify fetch) :error-hook utils/un-404)
                               #(-> fetch
                                    (fetcher 
                                     (utils/pathify fetch paths :version))
                                    (json/decode true)))
          (catch Exception e
            (if (instance? clojure.lang.ExceptionInfo e)
              (case (some-> e .data :status)
                404 (throw (Exception. 
                            ;; TODO: check for incorrect plugin path, maybe by testing the WRONG path to see if it succeeds?
                            (format
                             "Server on %s doesn't have the Migrator plugin installed or enabled. Enable it. Make sure it's installed in extend/addon/migrator not extend/addon/migrator-plugin too."
                             base-url)))
                401 (throw (Exception. 
                            (format
                             "You have the incorrect password set in the config file for the server at %s"
                             base-url)))
                (throw e))
              (throw e))))
        {:keys [hubzilla redmatrix plugin]} utils/min-supported-versions
        cleaned-platform (utils/clean-platform platform_version)]
    (log/info base-url "is running versions:" v)
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
                            "Server on %s has hubzilla version %s which is too old. Upgrade it to %d or higher."
                            base-url
                            platform_version
                            hubzilla))))
      "redmatrix" (when (< cleaned-platform redmatrix)
                    (throw (Exception. 
                            (format
                             "Server on %s has redmatrix version %s which is too old. Upgrade it to %d or higher."
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
