(ns ritzel.database
  (:require [mount.core :refer [defstate stop start]]
            [taoensso.timbre :as log]
            [datahike.api :as d]))

(def cfg
  {:store {:backend :file
           :path "/tmp/pulumi-db"}
   :name "pulumi-db"
   :keep-history? false
   :schema-flexibility :read})

(defn init-connections [db-config]
  (let [exists? (d/database-exists? db-config)]
    (when-not exists?
      (log/infof "Creating database...")
      (d/create-database db-config)
      (log/infof "Done"))
    (let [conn (d/connect db-config)]
      conn)))

(defstate connection
  :start (do
           (log/debug "Connecting to databases with config: " cfg)
           (init-connections cfg))
  :stop (d/release connection))

(defn cleanup-databases []
  (stop #'ritzel.database/connection)
  (log/info "Purging " cfg " ...")
  (d/delete-database cfg)
  (log/info "Done")
  (start #'ritzel.database/connection))

(comment
  (defn transact-stack [connection name org-name project-name]
    (-> (d/transact connection
                    [{:stack/name name
                      :stack/org-name org-name
                      :stack/project-name project-name}])
        (:tx-data)
        (first)
        (first)))

  (defn transact-tags [eid connection tags]
    (for [tag tags]
      (d/transact connection
                  [{:tag/name (key tag)
                    :tag/value (val tag)
                    :stack/_tags eid}])))

  (def tx1 (transact-stack connection "foo" "bar" "baz"))
  tx1
  (d/transact connection [{:tag/name "male"
                           :tag/value "fiz"
                           :stack/_tags tx1}])
  (transact-tags tx1 connection [{"foo" "bar"}{"baz" "meh"}])

  (clojure.core/key {:a 1})

  (clojure.pprint/pprint (d/pull @connection '[* {:stack/tags [*]}] 5))

  (d/transact connection [{:db/ident :stack/name
                           :db/valueType :db.type/string
                           :db/cardinality :db.cardinality/one}
                          {:db/ident :stack/org-name
                           :db/valueType :db.type/string
                           :db/unique :db.unique/identity
                           :db/cardinality :db.cardinality/one}
                          {:db/ident :stack/project-name
                           :db/valueType :db.type/string
                           :db/unique :db.unique/identity
                           :db/cardinality :db.cardinality/one}
                          {:db/ident :stack/tags
                           :db/valueType :db.type/ref
                           :db/cardinality :db.cardinality/many
                           :db/doc "The tags attributed to a stack"}
                          {:db/ident :tag/name
                           :db/valueType :db.type/string
                           :db/unique :db.unique/identity
                           :db/cardinality :db.cardinality/one}
                          {:db/ident :tag/value
                           :db/valueType :db.type/string
                           :db/unique :db.unique/identity
                           :db/cardinality :db.cardinality/one}
                          {:db/ident :update/uuid
                           :db/valueType :db.type/uuid
                           :db/unique :db.unique/identity
                           :db/cardinality :db.cardinality/one}
                          {:db/ident :update/value
                           :db/valueType :db.type/ref
                           :db/cardinality :db.cardinality/one
                           :db/doc "The values of an update"}])

  (d/transact connection
              [{:db/ident :stack/tags
                :db/valueType :db.type/ref
                :db/cardinality :db.cardinality/many
                :db/doc "The tags attributed to a stack"}])

  (d/transact connection
              [{:stack/name "test"
                :stack/org-name "foo"
                :stack/project-name "bar"}
               {:stack/name "test"
                :stack/org-name "foo"
                :stack/project-name "baz"}])

  (d/transact connection
              [{:db/id 1 :stack:name "helge"}])

  (d/transact connection
              [{:tag/name "helge"
                :tag/value "schneider"
                :stack/_tags 1}])

  (d/transact connection
              [{:tag/name "meister:lampe"
                :tag/value "gehtsnoch?"
                :stack/_tags 6}])

  (d/transact connection
              [{:stack/name "mark"
                :stack/org-name "oechler"
                :stack/project-name "derglubb"
                :stack/tags {:tag/name "meister:lampe"
                             :tag/value "gehtsnoch?"}}])

  (d/datoms @connection :eavt)
  (clojure.pprint/pprint (d/datoms @connection :eavt))
  (clojure.pprint/pprint (clojure.pprint/pprint (d/datoms @connection :eavt)))

  (clojure.pprint/pprint (d/q '[:find ?e ?a ?v
                                :where [?e ?a ?v]]
                              @connection))

  (clojure.pprint/pprint (d/q '[:find ?e ?uuid
                                :where [?e :update/uuid ?uuid]]
                              @connection))
  (d/pull @connection '[*] [:update/uuid "dcfb145e-3227-4c38-ae9b-04a18efee803"])

  (d/q '[:find ?e
         :where [?e :stack/project-name "meyer"]
         [?e :stack/org-name "gerhard"]]
       @connection)
  (d/q '[:find ?e
         :where [?e :stack/project-name "meyer"]
         [?e :stack/org-name "gerhard"]]
       @connection)

  (clojure.pprint/pprint (d/q '[:find [(pull ?e [*]) ...]
                                :where [?e ?a ?v]]
                              @connection))
  (clojure.pprint/pprint (d/q '[:find [(pull ?e [*]) ...]
                                :where [?e :update/uuid]]
                              @connection))
  (:stack/tags (d/entity @connection 7))
  (d/pull @connection '[*] 9)
  (d/pull @connection '[*] [:update/uuid #uuid "dcfb145e-3227-4c38-ae9b-04a18efee803"])
  (d/q '[:find [(pull ?e [* {:update/value [*]}])]
         :in $ ?uuid
         :where [?e :update/uuid  ?uuid]]
       @connection
       #uuid "dcfb145e-3227-4c38-ae9b-04a18efee803")
       
                      
  (d/pull @connection '[* {:stack/tags [*]}] 5)
  (clojure.pprint/pprint (d/pull @connection '[* {:stack/tags [*]}] 4))
  (clojure.pprint/pprint (d/pull @connection '[* {:update/value [*]}] 9))
  (d/pull @connection '[*] 11)
  (cleanup-databases)

  (def create-stack [{:db/ident :stack/name
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}
                     {:db/ident :stack/org-name
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}
                     {:db/ident :stack/project-name
                      :db/unique :db.unique/identity
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}
                     {:db/ident :stack/tags
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/many
                      :db/doc "The tags attributed to a stack"}])

  (def tag [{:db/ident :tag/name
             :db/valueType :db.type/string
             :db/cardinality :db.cardinality/one}
            {:db/ident :tag/value
             :db/valueType :db.type/string
             :db/cardinality :db.cardinality/one}])

  (def create-update [{:db/ident :stack/project-name
                       :db/valueType :db.type/string
                       :db/cardinality :db.cardinality/one}
                      {:db/ident :stack/runtime
                       :db/valueType :db.type/keyword
                       :db/cardinality :db.cardinality/one}
                      {:db/ident :stack/main
                       :db/valueType :db.type/string
                       :db/cardinality :db.cardinality/one
                       :db/doc "Main is the typical entrypoint for a resource provider plugin."}
                      {:db/ident :stack/description
                       :db/valueType :db.type/string
                       :db/cardinality :db.cardinality/one}
                      {:db/ident :stack/config
                       :db/valueType :db.type/tuple
                       :db/cardinality :db.cardinality/one}
                      {:db/ident :stack/options
                       :db/valueType :db.type/ref
                       :db/cardinality :db.cardinality/one
                       :db/doc "UpdateOptions is the set of operations for configuring the output of an update."}])

  (def stack-options [:stack:local-policy-pack-paths
                      :color
                      :dry-run
                      :parallel
                      :show-config
                      :show-replacement-steps
                      :show-names
                      :summary
                      :debug])

  (def renew-update-lease [{:db/ident :lease:token
                            :db/valueType :db.type/uuid
                            :db/cardinality :db.cardinality/one}
                           {:db/ident :lease:duration
                            :db/valueType :db.type/int
                            :db/cardinality :db.cardinality/one}])
  (def patch-checkpoint [:is-invalid boolean?
                         :version int?
                         :deployment {:manifest map?
                                      :secrets_providers map?
                                      :resources vector?
                                      :pending_operations vector?}])
  (def post-engine-events-batch {:events []}))
