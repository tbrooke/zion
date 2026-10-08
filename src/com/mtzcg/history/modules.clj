(ns com.mtzcg.history.modules
  (:require [com.biffweb.background :as biff.background]
            [com.biffweb.config :as biff.config]
            [com.biffweb.datastar :as biff.datastar]
            [com.biffweb.fx :as biff.fx]
            [com.biffweb.graph :as biff.graph]
            [com.biffweb.ring :as biff.ring]
            [com.mtzcg.history.app.admin :as app.admin]
            [com.mtzcg.history.app.auth :as app.auth]
            [com.mtzcg.history.app.home :as app.home]
            [com.mtzcg.history.app.items :as app.items]
            [com.mtzcg.history.app.landing :as app.landing]
            [com.mtzcg.history.lib.ui :as lib.ui]
            [com.mtzcg.history.model.seed :as model.seed]
            [com.mtzcg.history.model.sqlite :as model.sqlite]
            [com.mtzcg.history.model.user :as model.user]))

(def modules
  [{:biff.core/init {:biff.ring/on-error #'lib.ui/on-error}}
   (biff.config/module)
   (biff.ring/module)
   (biff.datastar/module)
   (biff.background/module)
   (biff.fx/module)
   (biff.graph/module)
   model.user/module
   model.sqlite/module
   model.seed/module
   app.admin/module
   app.landing/module
   app.auth/module
   app.home/module
   app.items/module])

(def start-order
  [:biff.config/module
   :biff.sqlite/module
   :history.seed/module
   :biff.admin/module
   :biff.background/module
   :biff.ring/module])
