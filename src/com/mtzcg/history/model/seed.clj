(ns com.mtzcg.history.model.seed
  (:require [com.biffweb.sqlite :as biff.sqlite]))

(def default-kinds
  ["Photograph" "Bulletin" "Newsletter" "Letter" "Minutes" "Program"
   "Document" "Video"])

(defn seed-kinds!
  "Inserts any missing default kinds. Safe to run on every start."
  [ctx]
  (doseq [kind-name default-kinds]
    (biff.sqlite/execute ctx {:insert-into :kind
                              :values      [{:kind/id   (random-uuid)
                                             :kind/name kind-name}]
                              :on-conflict [:kind/name]
                              :do-nothing  true}))
  ctx)

(def module
  {:biff.core/id    :history.seed/module
   :biff.core/start seed-kinds!})
