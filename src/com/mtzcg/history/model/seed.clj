(ns com.mtzcg.history.model.seed
  (:require [com.biffweb.sqlite :as biff.sqlite]))

(def default-item-types
  ["Photograph" "Bulletin" "Newsletter" "Letter" "Minutes" "Program"
   "Document" "Video"])

(defn seed-item-types!
  "Inserts any missing default item types. Safe to run on every start."
  [ctx]
  (doseq [type-name default-item-types]
    (biff.sqlite/execute ctx {:insert-into :item-type
                              :values      [{:item-type/id   (random-uuid)
                                             :item-type/name type-name}]
                              :on-conflict [:item-type/name]
                              :do-nothing  true}))
  ctx)

(def module
  {:biff.core/id    :history.seed/module
   :biff.core/start seed-item-types!})
