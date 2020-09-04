(ns ritzel.handlers
  (:require [ritzel.database :as database]
            [datahike.api :as d]
            [taoensso.timbre :as log]))

(defn success
  ([data] {:status 200 :body data})
  ([] {:status 200}))

(defn get-current-user [request]
  (success {:githubLogin (name (:identity request))}))

(defn list-user-stacks [request]
  (success {:stacks [{:orgName "mopedtobias"
                      :projectName "foobar"
                      :stackName "muh"}]}))

(defn get-cli-version-info [request]
  (success {:latestVersion "2.9.0"
            :oldestWithoutWarning "2.9.0"}))

(defn list-organization-stacks
  "Seems not implemented
   https://github.com/pulumi/pulumi/blob/66bd3f4aa8f9a90d3de667828dda4bed6e115f6b/pkg/cmd/pulumi/stack_ls.go"
  [request]
  (success))

(defn create-stack [{:keys [db-connection body-params path-params]}]
  ;; TODO transact correctly
  (let [stack-name    (:stackName body-params)
        org-name     (:org-name path-params)
        project-name (:project-name path-params)
        _ (log/info stack-name org-name project-name)
        _ (d/transact db-connection [{:stack:name stack-name
                                      :stack:org-name org-name
                                      :stack:project-name project-name}])]
    (success {})))

(defn get-stack [{{:keys [org-name project-name stack-name]} :path-params db-connection :db-connection}]
  ;; TODO query
  ;;(let [query (d/q '[:find ?e
  ;;                   :where
  ;;                   [?e :stack:name stack-name]
  ;;                   [?e :stack:project-name project-name]
  ;;                   [?e :stack:org-name org-name]
  ;;                 @db-connection}]
  (success
   {:activeUpdate ""
    :orgName "mopedtobias"
    :projectName "foobar"
    :stackName "dev"
    :tags {":gitHub:owner" "mopedtobias"
           ":pulumi:description" "A minimal Azure Python program"
           ":pulumi:runtime" "python"}}))

(defn delete-stack [request]
  ;; TODO retract
  {:status 204})

(defn export-stack [request]
  (success {:deployment {:manifest {:magic ""
                                    :time  "0001-01-01T00:00:00Z"
                                    :version ""}}
            :version 1}))

(defn import-stack [request]
  (success {:updateId "24f1a28a-0549-4848-b35d-f986d05c6b1b"}))

(defn get-stack-updates [request] ;TODO can be empty as well
  (success {:updates [{:config {},
                       :endTime 1598622551,
                       :environment {},
                       :kind "import",
                       :message "",
                       :resourceChanges {:create 0
                                         :delete 0
                                         :same 0
                                         :update 0}
                       :result "succeeded",
                       :startTime 1598622551,
                       :version 0}]}))

;; TODO version can be latest or int
;; returns https://github.com/pulumi/pulumi/blob/master/sdk/go/common/apitype/history.go#L84
(defn get-stack-update [{{:keys [org-name project-name stack-name version]} :path-params db-connection :db-connection}]
;TODO 404 on empty TODO get version number from DB TODO when version=latest get latest update
  (success {:info {:updates [{:config {},
                              :endTime 1598622551,
                              :environment {},
                              :kind "import",
                              :message "",
                              :resourceChanges {:create 0
                                                :delete 0
                                                :same 0
                                                :update 0}
                              :result "succeeded",
                              :startTime 1598622551,
                              :version 0}]}}))

;; TODO create update of kind update, preview, refresh, rename, destroy or import
;; https://github.com/pulumi/pulumi/blob/master/sdk/go/common/apitype/history.go#L23
(defn update-stack [{{:keys [org-name project-name stack-name update-kind]} :path-params
                     db-connection :db-connection}]
  (success {:updateID "f5c927c1-fb83-4e27-89f7-0d4f6fe16eec"}))

;; TODO check if update either succeeded, failed, running, requested or not started
;; https://github.com/pulumi/pulumi/blob/master/sdk/go/common/apitype/updates.go#L118
(defn get-update-status [{{:keys [org-name project-name stack-name update-kind update-id]} :path-params
                          db-connection :db-connection}]
  (success {:status "succeeded"}))

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
                                    :stack:project-name "foobar"}])
  (println (d/q '[:find ?e
                  :where
                  [?e :stack:name "moh"]
                  [?e :stack:org-name "mopedtobias"]
                  [?e :stack:project-name "foobar"]]
                @database/connection)))

