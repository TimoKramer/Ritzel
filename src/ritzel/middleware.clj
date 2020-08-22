(ns ritzel.middleware
  (:require
    [buddy.auth :refer [authenticated?]]
    [buddy.auth.backends :as buddy-auth-backends]
    [buddy.auth.middleware :as buddy-auth-middleware]))

(defn auth
  "Middleware used in routes that require authentication. If request is not
   authenticated a 401 not authorized response will be returned"
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      {:status 401 :error "Not authorized"})))

;; Define a in-memory relation between tokens and users:
(def tokens {:2f904e245c1f5 :admin
             :45c1f5e3f05d0 :foouser})

;; Define an authfn, function with the responsibility
;; to authenticate the incoming token and return an
;; identity instance

(defn my-authfn
  [request token]
  (let [token (keyword token)]
    (get tokens token nil)))

;; Create an instance
(def token-backend (buddy-auth-backends/token {:authfn my-authfn}))

(defn token-auth
  "Middleware used on routes requiring token authentication"
  [handler]
  (buddy-auth-middleware/wrap-authentication handler token-backend))
