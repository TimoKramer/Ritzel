(ns ritzel.server
  (:require [ritzel.handlers :as handlers]
            [ritzel.config :as config]
            [ritzel.middleware :as middleware]
            [ritzel.database :as database]
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

(def authorization-regex #"^token .*")
(s/def ::authorization (s/and string? #(re-matches authorization-regex %)))
(s/def ::authorization-header (s/keys :req-un [::authorization]))

(s/def ::stackName string?)
(s/def ::projectName string?)
(s/def ::orgName string?)
(s/def ::version int?)
(s/def ::activeUpdate string?)
(s/def ::tags (s/map-of string? string?))
(s/def ::create-stack-body (s/keys :req-un [::stackName]
                                   :opt-un [::tags]))

(s/def ::stack (s/keys :req-un [::stackName ::projectName ::orgName]
                       :opt-un [::tags ::version ::activeUpdate]))
(s/def ::stacks (s/coll-of ::stack))

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
                :parameters {:header ::authorization-header}
                :responses {200 {:body {:githubLogin string?}}}
                :middleware [middleware/token-auth middleware/auth]
                :handler handlers/get-current-user}}]
    ["/stacks"
     {:swagger {:tags ["user" "API"]}
      :get     {:summary "List user stacks."
                :parameters {:header ::authorization-header}
                :responses {200 {:body {:stacks ::stacks}}}
                :middleware [middleware/token-auth middleware/auth]
                :handler handlers/list-user-stacks}}]]
   ["/stacks"
    ["/:org-name/:project-name"
     {:swagger {:tags ["stacks" "API"]}
      :get     {:summary "List organization stacks."
                :parameters {:header ::authorization-header}
                :responses {200 {:body {:stacks ::stacks}}}
                :middleware [middleware/token-auth middleware/auth]
                :handler handlers/list-organization-stacks}
      :post    {:summary "Create stack."
                :parameters {:header ::authorization-header
                             :body ::create-stack-body}
                :responses {200 {:body map?}}
                :middleware [middleware/token-auth middleware/auth]
                :handler handlers/create-stack}}]]])
    ;;["/:org-name/:project-name"]
    ;;["/export"
    ;; {:swagger {:tags ["stacks" "API"]}
    ;;  :get     {:summary "Export stack."
    ;;            :middleware [middleware/token-auth middleware/auth]
    ;;            :handler handlers/export-stack}}]
    ;;["/import"
    ;; {:swagger {:tags ["stacks" "API"]}
    ;;  :post    {:summary "Import stack."
    ;;            :middleware [middleware/token-auth middleware/auth]
    ;;            :handler handlers/import-stack}}]
    ;;["/encrypt"
    ;; {:swagger {:tags ["stacks" "API"]}
    ;;  :post    {:summary "Encrypt value."
    ;;            :middleware [middleware/token-auth middleware/auth]
    ;;            :handler handlers/encrypt-value}}]
    ;;["/decrypt"
    ;; {:swagger {:tags ["stacks" "API"]}
    ;;  :post    {:summary "Decrypt value."
    ;;            :middleware [middleware/token-auth middleware/auth]
    ;;            :handler handlers/decrypt-value}}]
    ;;["/logs"
    ;; {:swagger {:tags ["stacks" "API"]}
    ;;  :get     {:summary "Get stack logs."
    ;;            :middleware [middleware/token-auth middleware/auth]
    ;;            :handler handlers/get-stack-logs}}]
    ;;["/updates"
    ;; {:swagger {:tags ["stacks" "API"]}
    ;;  :get     {:summary "Get stack updates."
    ;;            :middleware [middleware/token-auth middleware/auth]
    ;;            :handler handlers/get-stack-updates}}
    ;; ["/latest"
    ;;  {:swagger {:tags ["stacks" "API"]}
    ;;   :get     {:summary "Get latest stack update."
    ;;             :middleware [middleware/token-auth middleware/auth]
    ;;             :handler handlers/get-latest-stack-update}}]
    ;; ["/:version"
    ;;  {:swagger {:tags ["stacks" "API"]}
    ;;   :get     {:summary "Get stack update."
    ;;             :middleware [middleware/token-auth middleware/auth]
    ;;             :handler handlers/get-stack-update}}
    ;;  ["/contents"
    ;;   ["/files"
    ;;    {:swagger {:tags ["stacks" "API"]}
    ;;     :get     {:summary "Get update contents files."
    ;;               :middleware [middleware/token-auth middleware/auth]
    ;;               :handler handlers/get-update-contents-files}}]
    ;;   ["/file/*path"
    ;;    {:swagger {:tags ["stacks" "API"]}
    ;;     :get     {:summary "Get update contents file path."
    ;;               :middleware [middleware/token-auth middleware/auth]
    ;;               :handler handlers/get-update-contents-file-path}}]]]]
    ;;["/destroy"
    ;; {:swagger {:tags ["stacks" "API"]}
    ;;  :post    {:summary "Destroy stack."
    ;;            :middleware [middleware/token-auth middleware/auth]
    ;;            :handler handlers/create-destroy}}]
    ;;["/preview"
    ;; {:swagger {:tags ["stacks" "API"]}
    ;;  :post    {:summary "Preview stack."
    ;;            :middleware [middleware/token-auth middleware/auth]
    ;;            :handler handlers/create-preview}}]
    ;;["/update"
    ;; {:swagger {:tags ["stacks" "API"]}
    ;;  :post    {:summary "Update stack."
    ;;            :middleware [middleware/token-auth middleware/auth]
    ;;            :handler handlers/create-update}}]
    ;;["/:update-kind/:update-id"
    ;; {:swagger {:tags ["stacks" "API"]}
    ;;  :get     {:summary "Get update status."
    ;;            :middleware [middleware/token-auth middleware/auth]
    ;;            :handler handlers/get-update-status}}
    ;; {:swagger {:tags ["stacks" "API"]}
    ;;  :post    {:summary "Start update."
    ;;            :middleware [middleware/token-auth middleware/auth]
    ;;            :handler handlers/start-update}}
    ;; ["/checkpoint"
    ;;  {:swagger {:tags ["stacks" "API"]}
    ;;   :patch   {:summary "Patch checkpoint."
    ;;             :middleware [middleware/token-auth middleware/auth]
    ;;             :handler handlers/patch-checkpoint}}]
    ;; ["/complete"
    ;;  {:swagger {:tags ["stacks" "API"]}
    ;;   :post    {:summary "Complete update."
    ;;             :middleware [middleware/token-auth middleware/auth]
    ;;             :handler handlers/complete-update}}]
    ;; ["/events"
    ;;  {:swagger {:tags ["stacks" "API"]}
    ;;   :post    {:summary "Post engine event."
    ;;             :middleware [middleware/token-auth middleware/auth]
    ;;             :handler handlers/post-engine-event}}
    ;;  ["/batch"
    ;;   {:swagger {:tags ["stacks" "API"]}
    ;;    :patch   {:summary "Post engine event batch."
    ;;              :middleware [middleware/token-auth middleware/auth]
    ;;              :handler handlers/post-engine-event-batch}}]]
    ;; ["/renew_lease"
    ;;  {:swagger {:tags ["stacks" "API"]}
    ;;   :patch   {:summary "Renew lease."
    ;;             :middleware [middleware/token-auth middleware/auth]
    ;;             :handler handlers/renew-lease}}]]


(defn wrap-db-connection [handler]
  (fn [request]
    (handler (assoc request :db-connection database/connection))))

(comment (val (first database/connection)))

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
        (ring/create-default-handler)))
      wrap-db-connection))

(defn start-server [config]
  (run-jetty app (:server config)))

(defstate server
  :start (do
           (log/debug "Starting server")
           (start-server config/config))
  :stop (.stop server))
