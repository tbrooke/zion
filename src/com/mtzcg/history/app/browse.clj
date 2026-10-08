(ns com.mtzcg.history.app.browse
  (:require [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzcg.history.lib.dates :as dates]
            [com.mtzcg.history.lib.middleware :as mid]
            [com.mtzcg.history.lib.queries :as queries]
            [com.mtzcg.history.lib.ui :as ui]
            [com.mtzcg.history.routes :as routes]))

(defn- blank->nil [s]
  (when-not (str/blank? s) (str/trim s)))

(defn- range-ints
  "Turns \"1860\" / \"1865\" (partial dates) into [from to] YYYYMMDD ints
  covering both whole periods."
  [from to]
  (let [[f-start f-end] (dates/parse-partial-date from)
        [t-start t-end] (dates/parse-partial-date to)]
    [(or f-start (when-not to t-start))
     (or t-end (when-not to f-end))]))

(defn- fmt-ymd [n]
  (when n
    (format "%04d-%02d-%02d" (quot n 10000) (mod (quot n 100) 100) (mod n 100))))

(defn- saved-searches [ctx]
  (biff.sqlite/execute ctx {:select   [:saved-search/id :saved-search/name
                                       :saved-search/date-from :saved-search/date-to]
                            :from     :saved-search
                            :order-by [:saved-search/date-from :saved-search/name]}))

(defn- find-items [ctx date-from date-to]
  (->> (biff.sqlite/execute
        ctx {:select   queries/item-summary-select
             :from     :item
             :join     [:item-type [:= :item-type/id :item/item-type-id]]
             :where    (cond-> [:and]
                         date-from (conj [:>= :item/date-end date-from])
                         date-to   (conj [:<= :item/date-start date-to]))
             :order-by [:item/date-start :item/title]
             :limit    200})
       (queries/with-collections ctx)))

(defn- search-chip [active-id {:saved-search/keys [id name date-from date-to]}]
  [:span {:class ["inline-flex items-center overflow-hidden rounded-full border text-sm"
                  (if (= id active-id) "border-blue-600 bg-blue-600 text-white" "border-gray-400")]}
   [:a {:class ["px-3 py-1"]
        :title (str (fmt-ymd date-from) " to " (fmt-ymd date-to))
        :href  (str (routes/browse) "?search=" id)}
    name]
   [:form {:method "post" :action (routes/search-delete id) :class ["flex"]}
    [:button {:type  "submit"
              :title "Delete saved search"
              :class ["px-2 py-1 opacity-60 hover:opacity-100"]}
     "×"]]])

(defn browse-page [{:keys [params] :as request}]
  (let [searches            (saved-searches request)
        active              (some-> (:search params) parse-uuid)
        saved               (some #(when (= active (:saved-search/id %)) %) searches)
        from-str            (:from params)
        to-str              (:to params)
        [date-from date-to] (if saved
                              [(:saved-search/date-from saved) (:saved-search/date-to saved)]
                              (range-ints (blank->nil from-str) (blank->nil to-str)))
        items               (find-items request date-from date-to)]
    (ui/app-page
     request
     [:main {:id "browse-page" :class ["mx-auto flex w-full max-w-4xl flex-col gap-5 p-6"]}
      [:div {:class ["flex items-baseline justify-between"]}
       [:h1 {:class ["text-2xl font-semibold"]} "Browse items"]
       [:div {:class ["flex gap-4 text-sm"]}
        [:a {:class ["text-blue-600 hover:underline"] :href (routes/people)} "People"]
        [:a {:class ["text-blue-600 hover:underline"] :href (routes/item-new)} "Add item"]
        [:a {:class ["text-blue-600 hover:underline"] :href (routes/app)} "Home"]]]

      [:div {:class ["flex flex-wrap items-center gap-2"]}
       [:span {:class ["text-sm font-medium"]} "Saved searches:"]
       (if (seq searches)
         (for [s searches] (search-chip active s))
         [:span {:class ["text-sm text-gray-500"]} "none yet"])]

      [:form {:method "get"
              :action (routes/browse)
              :class  ["flex flex-wrap items-end gap-3 rounded border"
                       "border-gray-200 bg-gray-50 p-3"]}
       [:label {:class ["flex flex-col gap-1 text-sm font-medium"]}
        "From"
        [:input {:type        "text"
                 :name        "from"
                 :value       from-str
                 :placeholder "1860"
                 :class       ["w-36 rounded border border-gray-300 p-2"]}]]
       [:label {:class ["flex flex-col gap-1 text-sm font-medium"]}
        "To"
        [:input {:type        "text"
                 :name        "to"
                 :value       to-str
                 :placeholder "1865"
                 :class       ["w-36 rounded border border-gray-300 p-2"]}]]
       [:button {:type "submit" :class ["rounded bg-gray-800 px-3 py-2 text-sm text-white"]}
        "Search"]]

      (when (and (not saved) (or date-from date-to))
        [:form {:method "post"                                     :action (routes/searches)
                :class  ["flex flex-wrap items-end gap-3 text-sm"]}
         [:input {:type "hidden" :name "from" :value (or from-str "")}]
         [:input {:type "hidden" :name "to" :value (or to-str "")}]
         [:label {:class ["flex flex-col gap-1 font-medium"]}
          "Save this search as"
          [:input {:type        "text"
                   :name        "name"
                   :required    true
                   :placeholder "Civil War"
                   :class       ["rounded border border-gray-300 p-2"]}]]
         [:button {:type "submit" :class ["rounded border border-gray-400 px-3 py-2"]} "Save"]])

      [:p {:class ["text-sm text-gray-600"]}
       (count items) " item" (when (not= 1 (count items)) "s")
       (when (or date-from date-to)
         (str " between " (or (fmt-ymd date-from) "the beginning")
              " and " (or (fmt-ymd date-to) "today")))]

      (ui/item-list items)])))

(defn create-search [{:keys [params] :as ctx}]
  (let [name*     (blank->nil (:name params))
        [from to] (range-ints (blank->nil (:from params)) (blank->nil (:to params)))
        id        (random-uuid)
        now       (java.time.Instant/now)]
    (if-not (and name* (or from to))
      {:status 303 :headers {"location" (routes/browse)}}
      (do
        (biff.sqlite/execute ctx {:insert-into   :saved-search
                                  :values        [{:saved-search/id         id
                                                   :saved-search/name       name*
                                                   :saved-search/date-from  from
                                                   :saved-search/date-to    to
                                                   :saved-search/created-at now}]
                                  :on-conflict   [:saved-search/name]
                                  :do-update-set [:saved-search/date-from :saved-search/date-to]})
        {:status  303
         :headers {"location" (str (routes/browse) "?search="
                                   (:saved-search/id
                                    (first (biff.sqlite/execute
                                            ctx {:select [:saved-search/id]
                                                 :from   :saved-search
                                                 :where  [:= :saved-search/name name*]}))))}}))))

(defn delete-search [{:keys [path-params] :as ctx}]
  (biff.sqlite/execute ctx {:delete-from :saved-search
                            :where       [:= :saved-search/id (:id path-params)]})
  {:status 303 :headers {"location" (routes/browse)}})

(def module
  {:biff.ring/routes
   ["" {:middleware [mid/wrap-admin]}
    [(routes/browse)        {:get browse-page}]
    [(routes/searches)      {:post create-search}]
    [(routes/search-delete) {:post delete-search}]]})
