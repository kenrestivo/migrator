(ns migrator.munge-php)


(comment

  (->> "/home/cust/spaz/logs/hubzilla-a-object.json"
       slurp
       (#(json/decode % true))
       (clojure.walk/postwalk (fn [f] (if (string? f) (h/h f) f)))
       json/encode
       (spit "/tmp/foo.json"))


  (urepl/massive-spew "/tmp/foo.edn")


  )
