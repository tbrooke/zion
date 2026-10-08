(ns com.mtzcg.history.lib.storage
  "Content storage keyed by file UUID. The database holds the metadata and the
  storage key; the bytes live behind the Storage protocol so a local disk can
  later be swapped for S3 (or a clone) without touching callers."
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io File InputStream)
           (java.net URLConnection)
           (java.nio.file Files StandardCopyOption)
           (java.security MessageDigest)
           (java.util HexFormat UUID)))

(defprotocol Storage
  (put! [this key file] "Move/copy java.io.File `file` into storage at `key`.")
  (open [this key] "Returns an InputStream for `key`, or nil if missing.")
  (exists? [this key])
  (delete! [this key]))

(defn- safe-key [^String key]
  (when (or (str/includes? key "..") (str/starts-with? key "/"))
    (throw (ex-info "Invalid storage key" {:key key})))
  key)

(defrecord LocalStorage [^File root]
  Storage
  (put! [_ key file]
    (let [dest (io/file root (safe-key key))]
      (io/make-parents dest)
      (Files/copy (.toPath ^File file) (.toPath dest)
                  (into-array StandardCopyOption
                              [StandardCopyOption/REPLACE_EXISTING]))
      key))
  (open [_ key]
    (let [f (io/file root (safe-key key))]
      (when (.exists f) (io/input-stream f))))
  (exists? [_ key]
    (.exists (io/file root (safe-key key))))
  (delete! [_ key]
    (io/delete-file (io/file root (safe-key key)) true)))

(defn local-storage [root]
  (->LocalStorage (io/file root)))

;; Future: (defrecord S3Storage [client bucket] Storage ...) selected via config.
(defn storage
  "Builds the Storage for a Biff ctx (config key :history.storage/root)."
  [{:history.storage/keys [root]}]
  (local-storage (or root "storage/files")))

(defn extension [filename]
  (when-some [i (some-> filename (str/last-index-of "."))]
    (let [ext (str/lower-case (subs filename (inc i)))]
      (when (re-matches #"[a-z0-9]{1,8}" ext) ext))))

(defn storage-key
  "Sharded by the first two hex chars of the UUID: ab/ab12...-....ext"
  [^UUID id ext]
  (let [s (str id)]
    (str (subs s 0 2) "/" s (when ext (str "." ext)))))

(defn sha256-hex [^File file]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (with-open [^InputStream in (io/input-stream file)]
      (let [buf (byte-array 8192)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n)
              (.update md buf 0 n)
              (recur))))))
    (.formatHex (HexFormat/of) (.digest md))))

(defn guess-mime [filename]
  (or (some-> filename URLConnection/guessContentTypeFromName)
      "application/octet-stream"))

(defn store-file!
  "Stores `file` under a new (or given) file UUID. Returns a map of :file/*
  columns (without item-id/position/created-at) ready for insertion."
  [storage ^File file {:keys [id original-name mime-type]}]
  (let [id  (or id (random-uuid))
        key (storage-key id (extension original-name))]
    (put! storage key file)
    {:file/id            id
     :file/storage-key   key
     :file/sha256        (sha256-hex file)
     :file/mime-type     (or mime-type (guess-mime original-name))
     :file/size-bytes    (.length file)
     :file/original-name original-name}))
