(ns com.mtzcg.history.schema)

(def ? {:optional true})

(def columns
  {:user/id           {:type :uuid :primary-key true}
   :user/email        {:type :text :required true :unique true}
   :user/joined-at    {:type :inst :required true :index true}
   :user/display-name {:type :text}})

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
