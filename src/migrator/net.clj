(ns migrator.net
  (:require [clj-http.client :as client]

            [taoensso.timbre :as log]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

  (client/get url
              {:basic-auth [login pw]
               :throw-entire-message? true
               :as :json})

  (log/info "test")



  )
