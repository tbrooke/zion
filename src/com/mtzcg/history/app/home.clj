(ns com.mtzcg.history.app.home
  (:require [com.mtzcg.history.lib.middleware :as mid]
            [com.mtzcg.history.lib.ui :as ui]
            [com.mtzcg.history.routes :as routes]))

(defn home [request]
  (ui/app-page
   request
   [:main {:class ["flex flex-1 flex-col items-center gap-4 p-8"]}
    [:h1 {:class ["text-2xl font-semibold"]} "Mt Zion History"]
    [:a {:class ["text-blue-600 hover:underline"] :href (routes/admin)}
     "Admin dashboard"]
    [:form {:method "post" :action (routes/signout)}
     [:button {:class ["text-blue-600 hover:underline"] :type "submit"}
      "Sign out"]]]))

(def module
  {:biff.ring/routes
   ["" {:middleware [mid/wrap-signed-in]}
    [(routes/app) {:get home}]]})
