(defproject clojure-redis-boilerplate "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [cheshire "5.10.1"]
                 [com.taoensso/carmine "3.1.0"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [ring/ring-json "0.5.1"]
                 [ring-cors "0.1.8"]
                 [ring-transit "0.1.6"]
                 [compojure "1.6.2"]]
  :main ^:skip-aot clojure-redis-boilerplate.core
  :target-path "target/%s"
  :plugins [[lein-cljfmt "0.8.0"]]
  :profiles {:uberjar {:aot :all}})
