(ns ritzel.server
  (:require [ritzel.handlers :as handlers]
            [ritzel.config :as config]
            [ritzel.middleware :as middleware]
            [reitit.ring :as ring]
            [reitit.coercion.spec]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.coercion :as coercion]
            [reitit.dev.pretty :as pretty]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.middleware.dev :as dev]
            [muuntaja.core :as m]
            [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]
            [mount.core :refer [defstate]]
            [ring.adapter.jetty :refer [run-jetty]]))

(def routes
  ["/api"
   ["/swagger.json"
    {:get {:no-doc  true
           :swagger {:info {:title       "Ritzel API"
                            :description "Pulumi HTTP Backend"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/user"
    [""
    {:swagger {:tags ["user" "API"]}
     :get     {:summary "Get current user."
               :middleware [middleware/token-auth middleware/auth]
               :handler handlers/get-current-user}}]
    ["/stacks"
     {:swagger {:tags ["user" "API"]}
      :get     {:summary "List user stacks."
                :handler handlers/list-user-stacks}}]]
   #_["/stacks"
      ["/:org-name"
       ["/policypacks"
        {:swagger {:tags ["stacks" "API"]}
         :post    {:summary "Renew lease."
                   :middleware [middleware/token-auth middleware/auth]
                   :handler h/renew-lease}}]]
      ["/:project-name/:stack-name"
       {:swagger {:tags ["stacks" "API"]}
        :get     {:summary "List organization stacks."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/list-organization-stacks}}
       {:swagger {:tags ["stacks" "API"]}
        :post    {:summary "Create stack."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/create-stack}}]
      ["/export"
       {:swagger {:tags ["stacks" "API"]}
        :get     {:summary "Export stack."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/export-stack}}]
      ["/import"
       {:swagger {:tags ["stacks" "API"]}
        :post    {:summary "Import stack."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/import-stack}}]
      ["/encrypt"
       {:swagger {:tags ["stacks" "API"]}
        :post    {:summary "Encrypt value."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/encrypt-value}}]
      ["/decrypt"
       {:swagger {:tags ["stacks" "API"]}
        :post    {:summary "Decrypt value."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/decrypt-value}}]
      ["/logs"
       {:swagger {:tags ["stacks" "API"]}
        :get     {:summary "Get stack logs."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/get-stack-logs}}]
      ["/updates"
       {:swagger {:tags ["stacks" "API"]}
        :get     {:summary "Get stack updates."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/get-stack-updates}}
       ["/latest"
        {:swagger {:tags ["stacks" "API"]}
         :get     {:summary "Get latest stack update."
                   :middleware [middleware/token-auth middleware/auth]
                   :handler h/get-latest-stack-update}}]
       ["/:version"
        {:swagger {:tags ["stacks" "API"]}
         :get     {:summary "Get stack update."
                   :middleware [middleware/token-auth middleware/auth]
                   :handler h/get-stack-update}}
        ["/contents"
         ["/files"
          {:swagger {:tags ["stacks" "API"]}
           :get     {:summary "Get update contents files."
                     :middleware [middleware/token-auth middleware/auth]
                     :handler h/get-update-contents-files}}]
         ["/file/*path"
          {:swagger {:tags ["stacks" "API"]}
           :get     {:summary "Get update contents file path."
                     :middleware [middleware/token-auth middleware/auth]
                     :handler h/get-update-contents-file-path}}]]]]
      ["/destroy"
       {:swagger {:tags ["stacks" "API"]}
        :post    {:summary "Destroy stack."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/create-destroy}}]
      ["/preview"
       {:swagger {:tags ["stacks" "API"]}
        :post    {:summary "Preview stack."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/create-preview}}]
      ["/update"
       {:swagger {:tags ["stacks" "API"]}
        :post    {:summary "Update stack."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/create-update}}]
      ["/:update-kind/:update-id"
       {:swagger {:tags ["stacks" "API"]}
        :get     {:summary "Get update status."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/get-update-status}}
       {:swagger {:tags ["stacks" "API"]}
        :post    {:summary "Start update."
                  :middleware [middleware/token-auth middleware/auth]
                  :handler h/start-update}}
       ["/checkpoint"
        {:swagger {:tags ["stacks" "API"]}
         :patch   {:summary "Patch checkpoint."
                   :middleware [middleware/token-auth middleware/auth]
                   :handler h/patch-checkpoint}}]
       ["/complete"
        {:swagger {:tags ["stacks" "API"]}
         :post    {:summary "Complete update."
                   :middleware [middleware/token-auth middleware/auth]
                   :handler h/complete-update}}]
       ["/events"
        {:swagger {:tags ["stacks" "API"]}
         :post    {:summary "Post engine event."
                   :middleware [middleware/token-auth middleware/auth]
                   :handler h/post-engine-event}}
        ["/batch"
         {:swagger {:tags ["stacks" "API"]}
          :patch   {:summary "Post engine event batch."
                    :middleware [middleware/token-auth middleware/auth]
                    :handler h/post-engine-event-batch}}]]
       ["/renew_lease"
        {:swagger {:tags ["stacks" "API"]}
         :patch   {:summary "Renew lease."
                   :middleware [middleware/token-auth middleware/auth]
                   :handler h/renew-lease}}]]]])

(def route-opts
  {:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
   ;; :validate spec/validate ;; enable spec validation for route data
   ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
   :exception pretty/exception
   :data      {:coercion   reitit.coercion.spec/coercion
               :muuntaja   m/instance
               :middleware [swagger/swagger-feature
                            parameters/parameters-middleware
                            muuntaja/format-negotiate-middleware
                            muuntaja/format-response-middleware
                            exception/exception-middleware
                            muuntaja/format-request-middleware
                            coercion/coerce-response-middleware
                            coercion/coerce-request-middleware
                            multipart/multipart-middleware]}})

(def app
  (-> (ring/ring-handler
       (ring/router routes route-opts)
       (ring/routes
        (swagger-ui/create-swagger-ui-handler
         {:path   "/"
          :url    "/api/swagger.json"
          :config {:validatorUrl     nil
                   :operationsSorter "alpha"}})
        (ring/create-default-handler)))))

(defn start-server [config]
  (run-jetty app (:server config)))

(defstate server
  :start (do
           (log/debug "Starting server")
           (start-server config/config))
  :stop (.stop server))

