(ns com.mtzcg.history.app.people
  (:require [clojure.string :as str]
            [com.biffweb.sqlite :as biff.sqlite]
            [com.mtzcg.history.lib.middleware :as mid]
            [com.mtzcg.history.lib.queries :as queries]
            [com.mtzcg.history.lib.ui :as ui]
            [com.mtzcg.history.routes :as routes]))

(defn- blank->nil [s]
  (when-not (str/blank? s) (str/trim s)))

(defn people-page [request]
  (let [people (biff.sqlite/execute
                request
                {:select    [:person/id :person/name :person/notes
                             [[:count :item-person/id] :n]]
                 :from      :person
                 :left-join [:item-person [:= :item-person/person-id :person/id]]
                 :group-by  [:person/id]
                 :order-by  [:person/name :person/notes]})]
    (ui/app-page
     request
     [:main {:id "people-page" :class ["mx-auto flex w-full max-w-3xl flex-col gap-4 p-6"]}
      [:div {:class ["flex items-baseline justify-between"]}
       [:h1 {:class ["text-2xl font-semibold"]} "People"]
       [:a {:class ["text-sm text-blue-600 hover:underline"] :href (routes/browse)} "Browse"]]
      (if (seq people)
        [:ul {:class ["flex flex-col divide-y rounded border"]}
         (for [{:person/keys [id name notes] :keys [n]} people]
           [:li {:class ["flex items-baseline justify-between p-3"]}
            [:a {:class ["font-medium text-blue-600 hover:underline"] :href (routes/person id)}
             name (when notes [:span {:class ["font-normal text-gray-600"]} " — " notes])]
            [:span {:class ["text-sm text-gray-500"]} n (if (= 1 n) " item" " items")]])]
        [:p {:class ["text-gray-600"]}
         "No people yet. Add them from the Add item form."])])))

(defn person-page [{:keys [path-params] :as request}]
  (let [id     (:id path-params)
        person (first (biff.sqlite/execute
                       request {:select [:person/id :person/name :person/notes]
                                :from   :person
                                :where  [:= :person/id id]}))]
    (if-not person
      (ui/on-error (assoc request :status 404))
      (let [items (->> (biff.sqlite/execute
                        request
                        {:select   (conj queries/item-summary-select
                                         [:item-person/role :role])
                         :from     :item-person
                         :join     [:item [:= :item/id :item-person/item-id]
                                    :item-type [:= :item-type/id :item/item-type-id]]
                         :where    [:= :item-person/person-id id]
                         :order-by [:item/date-start :item/title]})
                       (queries/with-collections request))]
        (ui/app-page
         request
         [:main {:id "person-page" :class ["mx-auto flex w-full max-w-3xl flex-col gap-4 p-6"]}
          [:h1 {:class ["text-2xl font-semibold"]} (:person/name person)]
          [:form {:method "post" :action (routes/person-update id)
                  :class  ["flex flex-wrap items-end gap-3 text-sm"]}
           [:label {:class ["flex flex-col gap-1 font-medium"]}
            "Name"
            [:input {:type "text" :name "name" :required true :value (:person/name person)
                     :class ["rounded border border-gray-300 p-2"]}]]
           [:label {:class ["flex flex-col gap-1 font-medium"]}
            "Note"
            [:input {:type "text" :name "notes" :value (:person/notes person)
                     :class ["rounded border border-gray-300 p-2"]}]]
           [:button {:type "submit" :class ["rounded border border-gray-400 px-3 py-2"]} "Save"]]
          [:p {:class ["text-sm text-gray-600"]}
           (count items) " item" (when (not= 1 (count items)) "s")]
          (ui/item-list items)
          [:a {:class ["text-sm text-blue-600 hover:underline"] :href (routes/people)}
           "All people"]])))))

(defn update-person [{:keys [path-params params] :as ctx}]
  (when-some [name* (blank->nil (:name params))]
    (biff.sqlite/execute ctx {:update :person
                              :set    {:person/name  name*
                                       :person/notes (blank->nil (:notes params))}
                              :where  [:= :person/id (:id path-params)]}))
  {:status 303 :headers {"location" (routes/person (:id path-params))}})

(def module
  {:biff.ring/routes
   ["" {:middleware [mid/wrap-admin]}
    [(routes/people)        {:get people-page}]
    [(routes/person)        {:get person-page}]
    [(routes/person-update) {:post update-person}]]})
