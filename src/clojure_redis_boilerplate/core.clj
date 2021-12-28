(ns clojure-redis-boilerplate.core
  (:require 
    [taoensso.carmine :as carmine :refer [wcar]]
    [compojure.core :refer [context defroutes GET]]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.transit :as middleware]
    [ring.util.response :refer [redirect response]]
  )
  (:gen-class))

(defonce redis-connection {:pool {} :spec {:spec (System/getenv "REDIS_URL")}})

(defn set-key
  [key data]
  (wcar redis-connection (carmine/set key data)))

(defn get-key
  [key]
  (wcar redis-connection (carmine/get key)))

(defn del-key
  [key]
  (wcar redis-connection (carmine/del key)))

(defn wrap-response 
  [res & more]
  (-> (apply merge {:data res} more)
  (response)))

(defn get-key-route []
  (let [x (get-key "name")]
  (wrap-response x))
)

(defroutes key-routes
  (GET "/" [] (get-key-route))
  (route/not-found "Not found"))

(defroutes app-routes
  (context "/key" [] key-routes)
  (route/not-found "Not found"))

(def app
  ( -> 
    (handler/api app-routes)
    (middleware/wrap-transit-response {:encoding :json :opts {}})
    (wrap-cors :access-control-allow-origin #".*localhost.*"
               :access-control-allow-methods [:get]
               :access-control-allow-headers ["Origin" "X-Requested-With"
                                              "Content-Type" "Accept"
                                              "Cache-Control" "Accept-Encoding"])))

(defn -main
  [& args]
  (jetty/run-jetty #'app {:port 3001 :join? false}))