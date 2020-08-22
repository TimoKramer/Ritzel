(ns ritzel.database
  (:require [mount.core :refer [defstate stop start]]
            [taoensso.timbre :as log]
            [ritzel.config :refer [config]]
            [datahike.api :as d])
  (:import [java.util UUID]))

(defn init-connections [{:keys [databases]}]
  (if (nil? databases)
    (let [_ (when-not (d/database-exists?)
              (log/infof "Creating database...")
              (d/create-database)
              (log/infof "Done"))
          conn (d/connect)]
      {(-> @conn :config :name) conn})
    (reduce
     (fn [acc {:keys [name] :as cfg}]
       (when (contains? acc name)
         (throw (ex-info
                 (str "A database with name '" name "' already exists. Database names on the transactor should be unique.")
                 {:event :connection/initialization
                  :error :database.name/duplicate})))
       (when-not (d/database-exists? cfg)
         (log/infof "Creating database...")
         (d/create-database cfg)
         (log/infof "Done"))
       (let [conn (d/connect cfg)]
         (assoc acc (-> @conn :config :name) conn)))
     {}
     databases)))

(defstate conns
  :start (do
           (log/debug "Connecting to databases with config: " (str config))
           (init-connections config))
  :stop (for [conn (vals conns)]
          (d/release conn)))

(defn cleanup-databases []
  (stop #'ritzel.database/conns)
  (doall
   (for [cfg (:databases config)]
     (do
       (println "Purging " cfg " ...")
       (d/delete-database cfg)
       (println "Done"))))
  (start #'ritzel.database/conns))
