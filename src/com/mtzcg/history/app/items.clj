(ns com.mtzcg.history.app.items
  (:require [clojure.string :as str]
            [com.biffweb.datastar :as biff.datastar :refer [signal-name]]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzcg.history.lib.dates :as dates]
            [com.mtzcg.history.lib.middleware :as mid]
            [com.mtzcg.history.lib.storage :as storage]
            [com.mtzcg.history.lib.ui :as ui]
            [com.mtzcg.history.routes :as routes]))

;;;; Queries ===================================================================

(defn- all-item-types [ctx]
  (biff.sqlite/execute ctx {:select   [:item-type/id :item-type/name]
                            :from     :item-type
                            :order-by [:item-type/name]}))

(defn- all-collections [ctx]
  (biff.sqlite/execute ctx {:select   [:collection/id :collection/name :collection/category]
                            :from     :collection
                            :order-by [:collection/category :collection/name]}))

;;;; Form helpers ==============================================================

(defn- ->vec [x]
  (cond (nil? x) [] (sequential? x) (vec x) :else [x]))

(defn- split-names [s]
  (->> (str/split (or s "") #",")
       (map str/trim)
       (remove str/blank?)
       distinct
       vec))

(defn- blank->nil [s]
  (when-not (str/blank? s) (str/trim s)))

(defn- uploaded-files [params]
  (->> (->vec (:files params))
       (filter #(and (map? %) (pos? (or (:size %) 0))))
       vec))

(def ^:private input-class "w-full rounded border border-gray-300 p-2")
(def ^:private label-class "flex flex-col gap-1 text-sm font-medium")

;;;; New item page =============================================================

(defn- collection-chip [selected {:collection/keys [id name category]}]
  (let [id-str (str id)]
    [:label {:class ["cursor-pointer"] :title category}
     [:input {:type      "checkbox"
              :class     ["peer" "sr-only"]
              :name      "collection"
              :value     id-str
              :data-bind (signal-name :item/collections)
              :checked   (contains? selected id-str)}]
     [:span {:class ["inline-block rounded-full border border-gray-400 px-3 py-1 text-sm"
                     "peer-checked:border-blue-600 peer-checked:bg-blue-600"
                     "peer-checked:text-white peer-focus-visible:ring-2"]}
      name]]))

(defn- collection-picker [collections selected]
  [:div {:id "collection-picker" :class ["flex flex-col gap-2"]}
   [:div {:class ["text-sm font-medium"]} "Collections (pick at least one)"]
   [:div {:class ["flex flex-wrap items-center gap-2"]}
    (for [c collections]
      (collection-chip selected c))
    [:button {:type          "button"
              :class         ["rounded-full border border-dashed border-gray-400 px-3 py-1"
                              "text-sm hover:bg-gray-100"]
              :title         "New collection"
              :data-on:click "$_showAdd = !$_showAdd"}
     "+"]]
   [:div {:class     ["flex flex-wrap items-end gap-2 rounded border"
                      "border-gray-200 bg-gray-50 p-3"]
          :data-show "$_showAdd"
          :style     {:display "none"}}
    [:label {:class [label-class]}
     "New collection name"
     [:input {:type      "text"
              :class     ["rounded border border-gray-300 p-2"]
              :data-bind (signal-name :collection/new-name)}]]
    [:label {:class [label-class]}
     "Category"
     [:input {:type        "text"
              :list        "collection-categories"
              :class       ["rounded border border-gray-300 p-2"]
              :placeholder "Era, People, Activity..."
              :data-bind   (signal-name :collection/new-category)}]
     [:datalist {:id "collection-categories"}
      (for [cat (->> collections (keep :collection/category) distinct)]
        [:option {:value cat}])]]
    [:button {:type          "button"
              :class         ["rounded bg-gray-800 px-3 py-2 text-sm text-white"]
              :data-action   (routes/collections)
              :data-on:click "@post(el.dataset.action)"}
     "Add collection"]]])

(defn- new-item-form [{:keys [item-types collections selected error]}]
  [:form {:method  "post"
          :action  (routes/items)
          :enctype "multipart/form-data"
          :class   ["flex flex-col gap-5"]}
   (when error
     [:p {:class ["rounded bg-red-100 p-3 text-red-800"]} error])

   ;; data-ignore-morph keeps typed values when the page re-renders (e.g. after
   ;; a new collection is added).
   [:div {:data-ignore-morph true :class ["flex flex-col gap-5"]}
    [:label {:class [label-class]}
     "Files (scans, photos, video)"
     [:input {:type  "file"        :name "files" :multiple true :required true
              :class [input-class]}]]
    [:div {:class ["grid gap-5 sm:grid-cols-2"]}
     [:label {:class [label-class]}
      "Title"
      [:input {:type        "text"                            :name "title" :class [input-class]
               :placeholder "Defaults to the first file name"}]]
     [:label {:class [label-class]}
      "Item type"
      [:select {:name "item_type" :class [input-class]}
       (for [{:item-type/keys [id name]} item-types]
         [:option {:value (str id)} name])]]
     [:label {:class [label-class]}
      "Date"
      [:input {:type        "text"                        :name "date" :class [input-class]
               :placeholder "1952, 1952-06 or 1952-06-14"}]]
     [:label {:class [label-class]}
      "Date note"
      [:input {:type        "text"                       :name "date_note" :class [input-class]
               :placeholder "circa 1950, Summer 1962..."}]]]
    [:label {:class [label-class]}
     "Description"
     [:textarea {:name "description" :rows 4 :class [input-class]}]]]

   (collection-picker collections selected)

   [:div {:data-ignore-morph true :class ["flex flex-col gap-5"]}
    [:div {:class ["grid gap-5 sm:grid-cols-2"]}
     [:label {:class [label-class]}
      "People (comma separated)"
      [:input {:type "text" :name "people" :class [input-class]}]]
     [:label {:class [label-class]}
      "Their role"
      [:select {:name "role" :class [input-class]}
       [:option {:value "depicted"} "Depicted"]
       [:option {:value "creator"} "Creator (author / photographer)"]
       [:option {:value "recipient"} "Recipient"]
       [:option {:value "contributor"} "Contributor"]]]
     [:label {:class [label-class]}
      "Subjects (comma separated)"
      [:input {:type "text" :name "subjects" :class [input-class]}]]
     [:label {:class [label-class]}
      "Tags (comma separated)"
      [:input {:type "text" :name "tags" :class [input-class]}]]
     [:label {:class [label-class]}
      "Source"
      [:input {:type        "text"                      :name "source" :class [input-class]
               :placeholder "Where did this come from?"}]]
     [:label {:class [label-class]}
      "Rights"
      [:input {:type "text" :name "rights" :class [input-class]}]]]
    [:label {:class [label-class]}
     "Notes (private)"
     [:textarea {:name "notes" :rows 2 :class [input-class]}]]
    [:label {:class ["flex items-center gap-2 text-sm"]}
     [:input {:type "checkbox" :name "published" :value "true"}]
     "Published (visible on the public site)"]]

   [:div
    [:button {:type               "submit"
              :class              ["rounded bg-blue-600 px-4 py-2 text-white"
                                   "disabled:cursor-not-allowed disabled:opacity-40"]
              :data-attr:disabled "$item_collections.length === 0"}
     "Save item"]]])

(defn new-item-page [{:biff.datastar/keys [signals] :as request}]
  (let [selected (set (map str (:item/collections signals)))]
    (ui/app-page
     request
     [:main {:id    "new-item-page"
             :class ["mx-auto flex w-full max-w-3xl flex-col gap-6 p-6"]

             :data-signals__ifmissing
             (biff.datastar/signals-json {:item/collections        []
                                          :collection/new-name     ""
                                          :collection/new-category ""})}
      [:div {:data-signals__ifmissing "{_showAdd: false}"}
       [:h1 {:class ["text-2xl font-semibold"]} "Add item"]
       [:a {:class ["text-sm text-blue-600 hover:underline"] :href (routes/app)}
        "Back"]]
      (new-item-form {:item-types  (all-item-types request)
                      :collections (all-collections request)
                      :selected    selected
                      :error       (get-in request [:params :error])})])))

;;;; Actions ===================================================================

(defn add-collection
  "Creates a collection from the signals and selects it in the picker."
  [{:biff.datastar/keys [signals] :as ctx}]
  (let [coll-name (blank->nil (:collection/new-name signals))
        category  (blank->nil (:collection/new-category signals))
        selected  (->vec (:item/collections signals))]
    (if-not coll-name
      {:status 204}
      (let [existing (first (biff.sqlite/execute
                             ctx {:select [:collection/id]
                                  :from   :collection
                                  :where  [:= :collection/name coll-name]}))
            id       (or (:collection/id existing) (random-uuid))]
        (when-not existing
          (biff.sqlite/execute ctx {:insert-into :collection
                                    :values      [{:collection/id       id
                                                   :collection/name     coll-name
                                                   :collection/category category}]}))
        (biff.datastar/patch-signals
         {:item/collections        (vec (distinct (conj selected (str id))))
          :collection/new-name     ""
          :collection/new-category ""})))))

(defn- name-statements
  "Find-or-create rows in `table` for each name. Returns {:ids [...] :stmts [...]}
  where :stmts are insert statements for the names that don't exist yet."
  [ctx table names]
  (let [id-key   (keyword (name table) "id")
        name-key (keyword (name table) "name")
        existing (into {}
                       (map (juxt name-key id-key))
                       (when (seq names)
                         (biff.sqlite/execute ctx {:select [id-key name-key]
                                                   :from   table
                                                   :where  [:in name-key names]})))
        created  (into {} (comp (remove existing) (map (fn [n] [n (random-uuid)]))) names)]
    {:ids   (mapv #(or (existing %) (created %)) names)
     :stmts (mapv (fn [[n id]]
                    {:insert-into table :values [{id-key id name-key n}]})
                  created)}))

(defn- link-statements [table item-id other-key ids & [extra]]
  (let [prefix (name table)]
    (mapv (fn [other-id]
            {:insert-into table
             :values      [(merge {(keyword prefix "id")      (random-uuid)
                                   (keyword prefix "item-id") item-id
                                   other-key                  other-id}
                                  extra)]})
          ids)))

(def ^:private roles
  {"depicted"    :item-person.role/depicted
   "creator"     :item-person.role/creator
   "recipient"   :item-person.role/recipient
   "contributor" :item-person.role/contributor})

(defn create-item!
  "Stores the uploaded files and writes the item with all its relations.
  Returns the new item id."
  [{:keys [params biff.fx/now] :as ctx}]
  (let [files           (uploaded-files params)
        collections     (mapv parse-uuid (->vec (:collection params)))
        item-id         (random-uuid)
        now             (or now (java.time.Instant/now))
        date-str        (blank->nil (:date params))
        [d-start d-end] (dates/parse-partial-date date-str)
        store           (storage/storage ctx)
        stored          (map-indexed
                         (fn [i {:keys [tempfile filename content-type]}]
                           (assoc (storage/store-file!
                                   store tempfile
                                   {:original-name filename
                                    :mime-type     (when-not (str/blank? content-type)
                                                     content-type)})
                                  :file/item-id item-id
                                  :file/position i
                                  :file/created-at now))
                         files)
        people          (name-statements ctx :person (split-names (:people params)))
        subjects        (name-statements ctx :subject (split-names (:subjects params)))
        tags            (name-statements ctx :tag (split-names (:tags params)))
        title           (or (blank->nil (:title params))
                            (:file/original-name (first stored))
                            "Untitled")]
    (biff.sqlite/execute-tx
     ctx
     (concat
      [{:insert-into :item
        :values      [{:item/id           item-id
                       :item/item-type-id (parse-uuid (:item_type params))
                       :item/title        title
                       :item/description  (blank->nil (:description params))
                       :item/date-display (or (blank->nil (:date_note params)) date-str)
                       :item/date-start   d-start
                       :item/date-end     d-end
                       :item/source       (blank->nil (:source params))
                       :item/rights       (blank->nil (:rights params))
                       :item/notes        (blank->nil (:notes params))
                       :item/published    (= "true" (:published params))
                       :item/created-at   now
                       :item/updated-at   now}]}]
      (map (fn [f] {:insert-into :file :values [f]}) stored)
      (:stmts people) (:stmts subjects) (:stmts tags)
      (link-statements :item-collection item-id :item-collection/collection-id collections)
      (link-statements :item-person item-id :item-person/person-id (:ids people)
                       {:item-person/role
                        [:lift (get roles (:role params) :item-person.role/depicted)]})
      (link-statements :item-subject item-id :item-subject/subject-id (:ids subjects))
      (link-statements :item-tag item-id :item-tag/tag-id (:ids tags))))
    item-id))

(defn create-item [{:keys [params] :as ctx}]
  (cond
    (empty? (uploaded-files params))
    {:status 303 :headers {"location" (routes/item-new {:error "Choose at least one file."})}}

    (empty? (->vec (:collection params)))
    {:status 303 :headers {"location" (routes/item-new {:error "Pick at least one collection."})}}

    :else
    {:status  303
     :headers {"location" (routes/item (create-item! ctx))}}))

;;;; Item page + file serving ==================================================

(defn- item-details [ctx item-id]
  (let [item        (first (biff.sqlite/execute
                            ctx {:select [:item/* [:item-type/name :item-type-name]]
                                 :from   :item
                                 :join   [:item-type [:= :item-type/id :item/item-type-id]]
                                 :where  [:= :item/id item-id]}))
        files       (biff.sqlite/execute ctx {:select   [:file/id :file/mime-type
                                                         :file/original-name]
                                              :from     :file
                                              :where    [:= :file/item-id item-id]
                                              :order-by [:file/position]})
        collections (biff.sqlite/execute
                     ctx {:select [:collection/name]
                          :from   :item-collection
                          :join   [:collection [:= :collection/id
                                                :item-collection/collection-id]]
                          :where  [:= :item-collection/item-id item-id]})]
    (when item {:item item :files files :collections collections})))

(defn- file-view [{:file/keys [id mime-type original-name]}]
  (let [src (routes/file id)]
    (cond
      (str/starts-with? mime-type "image/")
      [:a {:href src} [:img {:src src :alt original-name :class ["max-h-96 rounded border"]}]]

      (str/starts-with? mime-type "video/")
      [:video {:src src :controls true :class ["max-h-96 rounded border"]}]

      :else
      [:a {:href src :class ["text-blue-600 hover:underline"]} (or original-name "Download")])))

(defn item-page [{:keys [path-params] :as request}]
  (if-some [{:keys [item files collections]} (item-details request (:id path-params))]
    (ui/app-page
     request
     [:main {:id "item-page" :class ["mx-auto flex w-full max-w-3xl flex-col gap-4 p-6"]}
      [:h1 {:class ["text-2xl font-semibold"]} (:item/title item)]
      [:p {:class ["text-sm text-gray-600"]}
       (:item-type-name item)
       (when-some [d (:item/date-display item)] (str " · " d))
       (if (:item/published item) " · Published" " · Draft")]
      [:div {:class ["flex flex-wrap gap-2"]}
       (for [{:collection/keys [name]} collections]
         [:span {:class ["rounded-full bg-blue-100 px-3 py-1 text-sm"]} name])]
      (when-some [d (:item/description item)] [:p d])
      [:div {:class ["flex flex-wrap gap-4"]} (map file-view files)]
      [:div {:class ["flex gap-4"]}
       [:a {:class ["text-blue-600 hover:underline"] :href (routes/item-new)} "Add another"]
       [:a {:class ["text-blue-600 hover:underline"] :href (routes/app)} "Home"]]])
    (ui/on-error (assoc request :status 404))))

(defn file-handler [{:keys [path-params] :as ctx}]
  (if-some [{:file/keys [storage-key mime-type]}
            (first (biff.sqlite/execute
                    ctx {:select [:file/storage-key :file/mime-type]
                         :from   :file
                         :where  [:= :file/id (:id path-params)]}))]
    (if-some [in (storage/open (storage/storage ctx) storage-key)]
      {:status  200
       :headers {"content-type" mime-type}
       :body    in}
      {:status 404})
    {:status 404}))

(def module
  {:biff.ring/routes
   ["" {:middleware [mid/wrap-admin]}
    [(routes/item-new)    {:get new-item-page}]
    [(routes/items)       {:post create-item}]
    [(routes/item)        {:get item-page}]
    [(routes/collections) {:post add-collection}]
    [(routes/file)        {:get file-handler}]]})
