(ns ritzel.handlers)

(defn success
  ([data] {:status 200 :body data})
  ([] {:status 200}))

(defn get-current-user [request]
  (success {:githublogin (name (:identity request))}))

(defn list-user-stacks [request]
  (success))
