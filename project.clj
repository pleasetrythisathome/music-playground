(defproject playground "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ^:replace []

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]

                 ;; music
                 [overtone "0.9.1"]
                 [monome-osc/monome-osc "0.1.0-SNAPSHOT"]

                 ;; systems utils
                 [brute "0.3.0"]
                 [throttler "1.0.0"]
                 [com.taoensso/encore "1.7.0"]

                 ;; web
                 [org.clojure/clojurescript "0.0-2268"]
                 [om "0.6.4"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {
                :output-to "main.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
