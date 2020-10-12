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
  (let [stack-name    (:stackName body-params)
        org-name     (:org-name path-params)
        project-name (:project-name path-params)
        tags         (:tags body-params)
        _ (log/debug "Creating stack with stack-name: " stack-name " org-name: " org-name " project-name: " project-name " and tags: " tags)
        eid (-> (d/transact db-connection [{:stack/name stack-name
                                            :stack/org-name org-name
                                            :stack/project-name project-name
                                            :stack/tags tags}])
                (:tx-data)
                (first)
                (first))
        response (d/pull @db-connection '[* {:stack/tags [*]}] eid)]
    (success response)))

(defn project-exists? [{{:keys [org-name project-name]} :path-params db-connection :db-connection}]
  (let [_ (log/debug "Querying for project with org: " org-name " project: " project-name)
        query (d/q '[:find ?e
                     :in $ ?org ?project
                     :where [?e :stack/project-name ?project]
                            [?e :stack/org-name ?org]]
                   @db-connection org-name project-name)
        _ (log/debug "Query result: " query)]
    (if (empty? query)
      {:status 404}
      (success))))

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

(defn update-tags [request]
  ;; TODO update tags in db
  {:status 204})

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
(defn get-stack-update [{{:keys [org-name project-name stack-name version]}
                         :path-params db-connection :db-connection}]
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
(defn -update-stack [{{:keys [org-name project-name stack-name] :as path-params} :path-params
                      body-params :body-params
                      db-connection :db-connection}
                     update-kind]
  (let [uuid (java.util.UUID/randomUUID)
        _ (d/transact db-connection [{:stack/name stack-name
                                      :stack/org-name org-name
                                      :stack/project-name project-name
                                      :update/uuid uuid
                                      :update/kind update-kind
                                      :update/value body-params}])]
    (success {:updateID uuid})))

(defn update-stack [params]
  (-update-stack params :update))

(defn destroy-stack [params]
  (-update-stack params :destroy))

(defn preview-stack [params]
  (-update-stack params :preview))

(comment
  (update-stack {:path-params {:foo :bar} :body-params {:foo :bar} :db-connection :foo}) 
  (uuid? (java.util.UUID/randomUUID))
  (d/transact database/connection [{:update/uuid "f5c927c1-fb83-4e27-89f7-0d4f6fe16eec"
                                    :update/body {:foo "bar"}}])
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
;; TODO check if update either succeeded, failed, running, requested or not started
;; https://github.com/pulumi/pulumi/blob/master/sdk/go/common/apitype/updates.go#L118
(defn get-update-status [{{:keys [org-name project-name stack-name update-kind update-id]} :path-params
                          db-connection :db-connection}]
  (success {:status "succeeded"}))

;; TODO implement handler
;; query-param "?continuationToken=%s"
;; retrieve update results
(defn get-update-events [{{:keys [org-name project-name stack-name update-kind update-id]} :path-params
                          db-connection :db-connection}]
  (success))

(defn start-update [{{:keys [org-name project-name stack-name update-kind update-id]} :path-params
                     {:keys [tags]} :body-params
                     db-connection :db-connection}]
  (let [_ (d/transact db-connection [{:stack/name stack-name
                                      :stack/org-name org-name
                                      :stack/project-name project-name
                                      :stack/tags tags
                                      :update/uuid update-id
                                      :update/kind update-kind}])])
    ;; TODO how to update tags?
    ;; TODO token format?
    ;; TODO increment version
  (success {:version 1 :token "eyJhbGciOiJIUzI1NiIsIm"}))

;; TODO store checkpoint in datahike, probably deployment as stringified json because cumbersome to spec
;; see doc/patchCheckpoint.json
;; invalidate checkpoint when payload empty
(defn patch-checkpoint [{{:keys [org-name project-name stack-name update-kind update-id]} :path-params
                         db-connection :db-connection}]
  {:status 204})

(defn complete-update [{{:keys [org-name project-name stack-name update-kind update-id]} :path-params
                        db-connection :db-connection}]
  {:status 204})

(defn cancel-update [{{:keys [org-name project-name stack-name update-kind update-id]} :path-params
                      db-connection :db-connection}]
  (success))

;; TODO store event-batch as stringified json for now? or maybe just not store it for MVP?
;; see doc/postEngineEventBatch.json
(defn post-engine-event-batch [{{:keys [org-name project-name stack-name update-kind update-id]} :path-params
                                db-connection :db-connection}]
  (success))

(defn renew-lease-token [{{:keys [org-name project-name stack-name update-kind update-id]} :path-params
                          {:keys [token duration]} :body
                          db-connection :db-connection}]
  ;; TODO create new token, store it in update-token-db, delete old token, return new token
  (success {:token :supergoodnewtoken4242}))



