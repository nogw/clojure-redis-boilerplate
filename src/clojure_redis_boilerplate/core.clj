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

(defmacro wcar* 
  [& body] 
  `(wcar redis-connection ~@body))

(defn set-key
  [key data]
  (wcar* (car/set key data (* 24 60 60))))

(defn get-key
  [key]
  (wcar* (car/get key)))

(defn del-key
  [key]
  (wcar* (car/del key)))

(defn wrap-error-response
  [res]
  (-> (json/generate-string {:error res}) (resp/response) (resp/status 400)))

(defn wrap-response
  [res & more]
  (-> (apply merge {:data res} more) (json/generate-string) (resp/response)))

(defn set-key-route
  [body]
  (let [key (body :key)
        value (body :value)]
  (try
    (set-key key value) 
    (catch Exception e
      (wrap-error-response (str e))))
  (wrap-response {:key key :value value})))

(defn get-key-route
  [key]
  (let [x (get-key key)]
  (wrap-response {:key key :value x})))

(defn del-key-route
  [key]
  (del-key key)
  (wrap-response "deleted"))

(defroutes routes
  (PUT    "/"     {body :body} (set-key-route body))
  (GET    "/:key" [key] (get-key-route key))
  (DELETE "/:key" [key] (del-key-route key))
  (route/not-found "Not found"))

(defroutes app-routes
  (context "/" [] routes)
  (route/not-found "Not found"))

(def app
  (->
   (handler/api app-routes)
   (wrap-json-body {:keywords? true :bigdecimals? true})
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