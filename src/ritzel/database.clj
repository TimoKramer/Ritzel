(ns ritzel.database
  (:require [mount.core :refer [defstate stop start]]
            [taoensso.timbre :as log]
            [datahike.api :as d]))

(def cfg
  {:store {:backend :file
           :path "/tmp/pulumi-db"}
   :name "pulumi-db"
   :keep-history? true
   :schema-flexibility :write})

(defn init-connections [db-config]
  (let [exists? (d/database-exists? db-config)]
    (when-not exists?
      (log/infof "Creating database...")
      (d/create-database db-config)
      (log/infof "Done"))
    (let [conn (d/connect db-config)]
      (d/transact conn [{:db/ident :stack:name
                               :db/valueType :db.type/string
                               :db/cardinality :db.cardinality/one}
                        {:db/ident :stack:org-name
                         :db/valueType :db.type/string
                         :db/cardinality :db.cardinality/one}
                        {:db/ident :stack:project-name
                         :db/valueType :db.type/string
                         :db/cardinality :db.cardinality/one}])
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
