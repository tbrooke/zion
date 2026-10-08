(ns com.mtzcg.history.lib.middleware
  (:require [com.mtzcg.history.routes :as routes]))

(defn wrap-signed-in [handler]
  (fn [{:keys [session] :as ctx}]
    (if (some? (:uid session))
      (handler ctx)
      {:status  303
       :headers {"location" (routes/signin)}})))

(defn wrap-redirect-signed-in [handler]
  (fn [{:keys [session] :as ctx}]
    (if (some? (:uid session))
      {:status  303
       :headers {"location" (routes/app)}}
      (handler ctx))))

(defn wrap-admin
  "Only lets through the user whose id is :biff.admin/admin-user-id."
  [handler]
  (fn [{:biff.admin/keys [admin-user-id] :keys [session] :as ctx}]
    (cond
      (nil? (:uid session))
      {:status 303 :headers {"location" (routes/signin)}}

      (not= (str (:uid session)) (str admin-user-id))
      {:status 403 :headers {"content-type" "text/html"} :body "<h1>Forbidden</h1>"}

      :else
      (handler ctx))))
