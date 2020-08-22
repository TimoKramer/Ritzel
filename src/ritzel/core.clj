(ns ritzel.core
  (:gen-class)
  (:require [mount.core :as mount]
            [taoensso.timbre :as log]
            [ritzel.config :refer [config]]
            [ritzel.database]
            [ritzel.server]))

(defn start-all []
  (mount/start))

(defn stop-all []
  (mount/stop))

(defn -main [& args]
  (mount/start)
  (log/info "Successfully loaded configuration: " (str config))
  (log/set-level! (get-in config [:server :loglevel]))
  (log/debugf "Ritzel Running!"))

(comment

  (mount/start)

  (mount/stop))
