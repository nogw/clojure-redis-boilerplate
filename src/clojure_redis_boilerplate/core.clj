(ns clojure-redis-boilerplate.core
  (:require
   [taoensso.carmine :as car :refer [wcar]]
   [compojure.core :refer [context defroutes GET POST DELETE]]
   [compojure.handler :as handler]
   [compojure.route :as route]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.transit :as middleware]
   [ring.middleware.json :refer [wrap-json-body]]
   [ring.util.response :as resp :refer [response]]
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

(defn wrap-response
  [res & more]
  (->
   (apply merge {:data res} more)
   (json/generate-string)
   (response)))

(defn set-key-route
  [req]
  (println (:body req))
  (let [j (:body req)]
    (set-key (j :key) (j :value))
    (wrap-response (:body req))))

(defn get-key-route
  [key]
  (let [x (get-key key)]
    (wrap-response {:key key :value x})))

(defn del-key-route
  [key]
  (del-key key)
  (wrap-response "deleted"))

(defroutes key-routes
  (GET "/:key" [key] (get-key-route key))
  (POST "/" [] (wrap-json-body set-key-route {:keywords? true :bigdecimals? true}))
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
              :access-control-allow-methods [:get]
              :access-control-allow-headers ["Origin" "X-Requested-With"
                                             "Content-Type" "Accept"
                                             "Cache-Control" "Accept-Encoding"])))

(defn -main
  [& _]
  (jetty/run-jetty #'app {:port 3013 :join? false}))