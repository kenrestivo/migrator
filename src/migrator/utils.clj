(ns migrator.utils
  (:import java.io.File))

;;; TODO: move to utilza
(defn directory-names
  "Returns seq of subdirectories"
  [directory-name]
  (for [f (->> directory-name File. .listFiles)
        :when (->> f .isDirectory  boolean)]
    (.getName f)))
