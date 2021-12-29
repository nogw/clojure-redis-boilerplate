(ns clojure-redis-boilerplate.core
  (:require
   [taoensso.carmine :as car :refer [wcar]]
   [compojure.core :refer [context defroutes GET PUT DELETE]]
   [compojure.handler :as handler]
   [compojure.route :as route]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.transit :as middleware]
   [ring.middleware.json :refer [wrap-json-body]]
   [ring.util.response :as resp]
   [cheshire.core :as json])
  (:gen-class))

(defonce redis-connection {:pool {} :spec {:spec (System/getenv "REDIS_URL")}})

(defn set-key
  [key data]
  (wcar redis-connection (car/set key data)))

(defn get-key
  [key]
  (wcar redis-connection (car/get key)))

(defn del-key
  [key]
  (wcar redis-connection (car/del key)))

(defn wrap-bad-response
  [res]
  (-> (json/generate-string {:error res}) (resp/response) (resp/status 400)))

(defn wrap-response
  [res & more]
  (-> (apply merge {:data res} more) (json/generate-string) (resp/response)))

(defn set-key-route
  [req]
  (let [request (:body req)
        key (request :key)
        value (request :value)]
    (try
      ((set-key key value))
      (catch Exception e
        ;; TODO: create error response
        (wrap-bad-response (str "-> " e))))
    (wrap-response {:key key :value value})))

(defn get-key-route
  [key]
  (let [x (get-key key)]
    (wrap-response {:key key :value x})))

(defn del-key-route
  [key]
  (del-key key)
  (wrap-response "deleted"))

(defroutes key-routes
  (PUT    "/" [] (wrap-json-body set-key-route {:keywords? true :bigdecimals? true}))
  (GET    "/:key" [key] (get-key-route key))
  (DELETE "/:key" [key] (del-key-route key))
  (route/not-found "Not found"))

(defroutes app-routes
  (context "/" [] key-routes)
  (route/not-found "Not found"))

(def app
  (->
   (handler/api app-routes)
   (middleware/wrap-transit-response {:encoding :json :opts {}})
   (wrap-cors :access-control-allow-origin #".*localhost.*"
              :access-control-allow-methods [:get :put :delete]
              :access-control-allow-headers ["Origin" "X-Requested-With"
                                             "Content-Type" "Accept"
                                             "Cache-Control" "Accept-Encoding"])))

(defn -main
  [& [port]]
  (let [port (Integer. (or port (System/getenv "PORT") 3000))]
    (jetty/run-jetty #'app {:port port :join? false})))