(ns ritzel.handlers
  (:require [ritzel.database :as database]
            [datahike.api :as d]
            [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]))

(defn success
  ([data] {:status 200 :body data})
  ([] {:status 200}))

(defn get-current-user [request]
  (success {:githublogin (name (:identity request))}))

(s/def ::org-name string?)
(s/def ::project-name string?)
(s/def ::stack-name string?)
(s/def ::last-update int?)
(s/def ::resource-count int?)
(s/def ::stack-summary (s/keys :req-un [::org-name ::project-name ::stack-name]
                               :opt-un [::last-update ::resource-count]))

(defn list-user-stacks [request]
  (success))

(defn list-organization-stacks [request]
  (success))

(defn create-stack [{:keys [db-connection body-params path-params]}]
  (let [stack-name    (:stackName body-params)
        org-name     (:org-name path-params)
        project-name (:project-name path-params)
        _ (log/info stack-name org-name project-name)
        _ (d/transact db-connection [{:stack:name stack-name
                                      :stack:org-name org-name
                                      :stack:project-name project-name}])]
    (success {})))

(comment
  (type (int 3))
  database/connection
  (def conn (get database/connection "pulumi-db"))
  (d/transact conn [{:db/ident :stack:name
                                    :db/valueType :db.type/string
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident :stack:org-name
                                    :db/valueType :db.type/string
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident :stack:project-name
                                    :db/valueType :db.type/string
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident :stack:last-update
                                    :db/valueType :db.type/long
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident :stack:resource-count
                                    :db/valueType :db.type/long
                                    :db/cardinality :db.cardinality/one}])
  (get database/connection "pulumi-db")
  conn
  (d/datoms :stack:name)
  (d/q '[:find ?e
         :where [_ _ ?e]]
       @database/connection)
  (d/transact database/connection [{:stack:name "foobar"
                                    :stack:org-name "foobar"
                                    :stack:project-name "foobar"}]))
