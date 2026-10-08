(ns com.mtzcg.history.lib.queries
  (:require [com.biffweb.sqlite :as biff.sqlite]))

(def item-summary-select
  [:item/id :item/title :item/date-display :item/date-start
   [:item-type/name :type-name]])

(defn with-collections
  "Adds a :collections seq of names to each item summary."
  [ctx items]
  (let [ids   (mapv :item/id items)
        colls (when (seq ids)
                (group-by :item-collection/item-id
                          (biff.sqlite/execute
                           ctx {:select [:item-collection/item-id :collection/name]
                                :from   :item-collection
                                :join   [:collection [:= :collection/id
                                                      :item-collection/collection-id]]
                                :where  [:in :item-collection/item-id ids]})))]
    (mapv #(assoc % :collections (map :collection/name (get colls (:item/id %)))) items)))
