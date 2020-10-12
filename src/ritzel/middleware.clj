(ns ritzel.middleware
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends :as buddy-auth-backends]
   [buddy.auth.middleware :as buddy-auth-middleware]))

(def tokens {:2f904e245c1f5 :current-user})

(defn token-authfn
  [request token]
  (let [token (keyword token)]
    (get tokens token nil)))

(def token-backend (buddy-auth-backends/token {:authfn token-authfn :token-name "token"}))

(defn token-auth
  "Middleware used on routes requiring token authentication"
  [handler]
  (buddy-auth-middleware/wrap-authentication handler token-backend))

(def update-tokens {:eyJhbGciOiJIUzI1NiIsIm :update-token})

(defn update-token-authfn
  [request token]
  (let [token (keyword token)]
    (get update-tokens token nil)))

(def update-token-backend (buddy-auth-backends/token {:authfn update-token-authfn :token-name "update-token"}))

;; TODO implement update-token, could be stored in a second mem store
(defn update-token-auth
  "Middleware used on routes requiring token authentication"
  [handler]
  (buddy-auth-middleware/wrap-authentication handler update-token-backend))

(defn auth
  "Middleware used in routes that require authentication. If request is not
   authenticated a 401 not authorized response will be returned"
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      {:status 401 :error "Not authorized"})))
