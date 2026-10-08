(ns com.mtzcg.history.schema)

(def ? {:optional true})

(def person-roles
  {1 :item-person.role/depicted
   2 :item-person.role/creator
   3 :item-person.role/recipient
   4 :item-person.role/contributor})

;; Loosely modelled on Omeka / Dublin Core. An item is the thing being
;; described (a photo, a bulletin issue); a file is a scan or image that
;; belongs to it (an item can have many, e.g. each page of a bulletin).
;;
;; Ways to organise items, all many-to-many:
;; - kind:       editable list of item types (Photograph, Bulletin, ...). One per item.
;; - collection: named, optionally nested groups. A `category` ("Era", "People",
;;               "Activity", ...) is free text, so eras like Civil War / WWI are just
;;               collections with start/end years, as are Pastors or John's River.
;; - subject:    curated, optionally nested vocabulary terms.
;; - tag:        free-form keywords.
;;
;; Dates are partial-friendly: date-display is free text ("circa 1950",
;; "Summer 1962"); date-start / date-end are YYYYMMDD ints used for sorting and
;; range search (a year-only date is 19500101 - 19501231).
(def columns
  {:user/id           {:type :uuid :primary-key true}
   :user/email        {:type :text :required true :unique true}
   :user/joined-at    {:type :inst :required true :index true}
   :user/display-name {:type :text}

   :kind/id          {:type :uuid :primary-key true}
   :kind/name        {:type :text :required true :unique true}
   :kind/description {:type :text}

   :collection/id          {:type :uuid :primary-key true}
   :collection/name        {:type :text :required true :unique true}
   :collection/category    {:type :text :index true}
   :collection/description {:type :text}
   :collection/parent-id   {:type :uuid :ref :collection/id :index true}
   :collection/start-year  {:type :int}
   :collection/end-year    {:type :int}

   :subject/id          {:type :uuid :primary-key true}
   :subject/name        {:type :text :required true :unique true}
   :subject/description {:type :text}
   :subject/parent-id   {:type :uuid :ref :subject/id :index true}

   :tag/id   {:type :uuid :primary-key true}
   :tag/name {:type :text :required true :unique true}

   :item/id           {:type :uuid :primary-key true}
   :item/kind-id      {:type :uuid :ref :kind/id :required true :index true}
   :item/title        {:type :text :required true}
   :item/description  {:type :text}
   :item/date-display {:type :text}
   :item/date-start   {:type :int :index true}
   :item/date-end     {:type :int}
   :item/source       {:type :text}
   :item/rights       {:type :text}
   :item/notes        {:type :text}
   :item/published    {:type :boolean :required true}
   :item/created-at   {:type :inst :required true}
   :item/updated-at   {:type :inst :required true}

   :file/id            {:type :uuid :primary-key true}
   :file/item-id       {:type :uuid :ref :item/id :required true :index true}
   :file/position      {:type :int :required true}
   :file/storage-key   {:type :text :required true :unique true}
   :file/sha256        {:type :text :required true :index true}
   :file/mime-type     {:type :text :required true}
   :file/size-bytes    {:type :int :required true}
   :file/original-name {:type :text}
   :file/created-at    {:type :inst :required true}

   :person/id    {:type :uuid :primary-key true}
   :person/name  {:type :text :required true :index true}
   :person/notes {:type :text}

   :item-person/id        {:type :uuid :primary-key true}
   :item-person/item-id   {:type :uuid :ref :item/id :required true :index true}
   :item-person/person-id {:type :uuid :ref :person/id :required true :index true}
   :item-person/role      {:type        :enum
                           :required    true
                           :enum-values person-roles
                           :unique-with [:item-person/item-id
                                         :item-person/person-id]}

   :item-collection/id {:type :uuid :primary-key true}

   :item-collection/item-id
   {:type :uuid :ref :item/id :required true :index true}

   :item-collection/collection-id
   {:type        :uuid
    :ref         :collection/id
    :required    true
    :index       true
    :unique-with [:item-collection/item-id]}

   :item-subject/id {:type :uuid :primary-key true}

   :item-subject/item-id
   {:type :uuid :ref :item/id :required true :index true}

   :item-subject/subject-id
   {:type        :uuid
    :ref         :subject/id
    :required    true
    :index       true
    :unique-with [:item-subject/item-id]}

   :item-tag/id {:type :uuid :primary-key true}

   :item-tag/item-id
   {:type :uuid :ref :item/id :required true :index true}

   :item-tag/tag-id
   {:type        :uuid
    :ref         :tag/id
    :required    true
    :index       true
    :unique-with [:item-tag/item-id]}})

;; Strings added here will be appended to resources/schema.sql
(def extra-init-sql [])

;; Add new columns here that you want users to be able to edit (e.g. settings).
(def editable-user-fields [:user/display-name])

(defn only-fields-edited? [before after fields]
  (= (apply dissoc before fields)
     (apply dissoc after fields)))

(defn authorize-entry [{{:keys [uid]} :session}
                       {:keys [table op before after]}]
  (case table
    :user
    (and (every? #{uid} (keep :user/id [before after]))
         (case op
           :create false
           :update (only-fields-edited? before after editable-user-fields)
           :delete true))

    false))

;; These authorization rules apply when using biff.sqlite/authorized-write and
;; are meant as an extra layer of protection.
(defn authorize
  [ctx diff]
  (every? #(authorize-entry ctx %) diff))
