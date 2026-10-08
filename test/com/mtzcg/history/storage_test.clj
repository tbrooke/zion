(ns com.mtzcg.history.storage-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [com.mtzcg.history.lib.storage :as storage])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(deftest store-and-read-back
  (let [dir  (.toFile (Files/createTempDirectory "hist" (make-array FileAttribute 0)))
        st   (storage/local-storage dir)
        src  (doto (io/file dir "src.txt") (spit "hello"))
        meta (storage/store-file! st src {:original-name "Scan 1.TXT"})]
    (is (= 5 (:file/size-bytes meta)))
    (is (= "text/plain" (:file/mime-type meta)))
    (is (re-matches #"[0-9a-f]{2}/[0-9a-f-]{36}\.txt" (:file/storage-key meta)))
    (is (= "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
           (:file/sha256 meta)))
    (is (= "hello" (slurp (storage/open st (:file/storage-key meta)))))
    (storage/delete! st (:file/storage-key meta))
    (is (not (storage/exists? st (:file/storage-key meta))))
    (is (thrown? Exception (storage/open st "../etc/passwd")))))
