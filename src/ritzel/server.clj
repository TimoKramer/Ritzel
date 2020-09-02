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
(s/def ::updateId string?)
(s/def ::ciphertext string?)
(s/def ::config map?)
(s/def ::endTime int?)
(s/def ::environment map?)
(s/def ::kind #{"import" "destroy" "rename" "refresh" "preview" "update"}) ;;https://github.com/pulumi/pulumi/blob/master/sdk/go/common/apitype/history.go#L25-L38)
(s/def ::message string?)
(s/def ::resourceChanges (s/map-of #{:create :delete :same :update} (s/and int? #(>= % 0)))) ;;https://github.com/pulumi/pulumi/blob/master/pkg/engine/update.go#L143)
(s/def ::result #{"in-progress" "succeeded" "failed"}) ;;https://github.com/pulumi/pulumi/blob/master/pkg/backend/updates.go#L35-L42)
(s/def ::startTime int?)
(s/def ::tags (s/map-of string? string?))
(s/def ::create-stack-body (s/keys :req-un [::stackName]
                                   :opt-un [::tags]))
(s/def ::stack (s/keys :req-un [::stackName ::projectName ::orgName]
                       :opt-un [::tags ::version ::activeUpdate]))
(s/def ::stacks (s/coll-of ::stack))
(s/def ::deployment map?)
(s/def ::untyped-deployment (s/keys :req-un [::deployment ::version]))
(s/def ::import-response (s/keys :req-un [::updateId]))
(s/def ::encrypt-decrypt (s/keys :req-un [::ciphertext]))
(s/def ::update (s/keys :req-un [::config ::endTime ::environment ::kind ::message ::resourceChanges ::result ::startTime ::version])) ;https://github.com/pulumi/pulumi/blob/master/sdk/go/common/apitype/history.go#L84
(s/def ::updates (s/or :nil nil? :update (s/map-of #{:updates} (s/coll-of ::update))))
(s/def ::info ::update)
(s/def ::info-update (s/keys :req-un [::info]))

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
   ["/cli/version"
    {:swagger {:tags ["cli" "API"]}
     :get     {:summary "Get information about versions of the CLI."
               :responses {200 {:body {:latestVersion string?
                                       :oldestWithoutWarning string?}}}
               :handler handlers/get-cli-version-info}}]
   ["/stacks"
   ;;TODO List Organization stacks seems not implemented
    ["/:org-name/:project-name"
     {:swagger {:tags ["stacks" "API"]}
      :post    {:summary "Create stack."
                :parameters {:header ::authorization-header
                             :body ::create-stack-body}
                :responses {200 {:body map?}}
                :middleware [middleware/token-auth middleware/auth]
                :handler handlers/create-stack}}
     ["/:stack-name"
      {:swagger {:tags ["stacks" "API"]}
       :get    {:summery "Get stack."
                :parameters {:header ::authorization-header}
                :responses {200 {:body ::stack}}
                :middleware [middleware/token-auth middleware/auth]
                :handler handlers/get-stack}
       :delete {:summary "Delete stack."
                :parameters {:header ::authorization-header}
                :responses {204 {}}
                :middleware [middleware/token-auth middleware/auth]
                :handler handlers/delete-stack}}
      ["/export"
       {:swagger {:tags ["stacks" "API"]}
        :get {:summary "Export stack."
              :parameters {:header ::authorization-header}
              :responses {200 {:body ::untyped-deployment}}
              :middleware [middleware/token-auth middleware/auth]
              :handler handlers/export-stack}}]
      ["/import"
       {:swagger {:tags ["stacks" "API"]}
        :post    {:summary "Import stack."
                  :parameters {:header ::authorization-header
                               :body ::untyped-deployment}
                  :responses {200 {:body ::import-response}}
                  :middleware [middleware/token-auth middleware/auth]
                  :handler handlers/import-stack}}]
      ; TODO might be good to use https://www.pulumi.com/docs/intro/concepts/config/#available-encryption-providers
      #_["/encrypt"
         {:swagger {:tags ["stacks" "API"]}
          :post    {:summary "Encrypt value."
                    :parameters {:header ::authorization-header
                                 :body string?}
                    :responses {200 {:body ::encrypt-decrypt}}
                    :middleware [middleware/token-auth middleware/auth]
                    :handler handlers/encrypt-value}}]
      #_["/decrypt"
         {:swagger {:tags ["stacks" "API"]}
          :post    {:summary "Decrypt value."
                    :parameters {:header ::authorization-header
                                 :body ::encrypt-decrypt}
                    :responses {200 {:body string?}}
                    :middleware [middleware/token-auth middleware/auth]
                    :handler handlers/decrypt-value}}]
      ; TODO Get Logs seems not implemented
      #_["/logs"
         {:swagger {:tags ["stacks" "API"]}
          :get     {:summary "Get stack logs."
                    :middleware [middleware/token-auth middleware/auth]
                    :handler handlers/get-stack-logs}}]
      ["/updates"
       {:swagger {:tags ["stacks" "API"]}
        :get     {:summary "Get stack updates."
                  :parameters {:header ::authorization-header}
                  :responses {200 {:body ::updates}}
                  :middleware [middleware/token-auth middleware/auth]
                  :handler handlers/get-stack-updates}}
       ["/:version" ;https://github.com/pulumi/pulumi/blob/master/sdk/go/common/apitype/history.go#L84
        {:swagger {:tags ["stacks" "API"]}
         :get     {:summary "Get stack update."
                   :parameters {:header ::authorization-header}
                   :responses {200 {:body ::info-update}}
                   :middleware [middleware/token-auth middleware/auth]
                   :handler handlers/get-stack-update}}
        #_["/contents"
         ["/files"
          {:swagger {:tags ["stacks" "API"]}
           :get     {:summary "Get update contents files."
                     :parameters {:header ::authorization-header}
                     :responses {200 {:body ::latest-update}}
                     :middleware [middleware/token-auth middleware/auth]
                     :handler handlers/get-update-contents-files}}]
         ["/file/*path"
          {:swagger {:tags ["stacks" "API"]}
           :get     {:summary "Get update contents file path."
                     :parameters {:header ::authorization-header}
                     :middleware [middleware/token-auth middleware/auth]
                     :handler handlers/get-update-contents-file-path}}]]]]]]]])

(defn wrap-db-connection [handler]
  (fn [request]
    (handler (assoc request :db-connection database/connection))))

(comment
  (val (first database/connection))
  (s/explain seq? {:foo "bar"})
  (println (s/valid? ::updates {:updates [{:config {},
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
                                           :version 0}]})))

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
