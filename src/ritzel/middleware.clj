(ns ritzel.middleware
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends :as buddy-auth-backends]
   [buddy.auth.middleware :as buddy-auth-middleware]))

(def tokens {:2f904e245c1f5 :mopedtobias})

(defn my-authfn
  [request token]
  (let [token (keyword token)]
    (get tokens token nil)))

(def token-backend (buddy-auth-backends/token {:authfn my-authfn :token-name "token"}))

(defn token-auth
  "Middleware used on routes requiring token authentication"
  [handler]
  (buddy-auth-middleware/wrap-authentication handler token-backend))

(defn auth
  "Middleware used in routes that require authentication. If request is not
   authenticated a 401 not authorized response will be returned"
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      {:status 401 :error "Not authorized"})))
