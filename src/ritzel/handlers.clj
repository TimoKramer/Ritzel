(ns ritzel.handlers
  (:require [ritzel.database :refer [conns]]
            [datahike.api :as d]
            [datahike.db :as dd]
            [datahike.core :as c]))

(defn success
  ([data] {:status 200 :body data})
  ([] {:status 200}))

(defn get-current-user []
  (success))

(defn list-user-stacks []
  (success))
