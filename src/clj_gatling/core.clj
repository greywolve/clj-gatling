(ns clj-gatling.core
  (:import (org.joda.time LocalDateTime))
  (:require [clojure-csv.core :as csv]
            [clj-gatling.chart :as chart]
            [clj-gatling.report :as report]
            [clj-gatling.simulation-util :refer [weighted-scenarios choose-runner]]
            [clj-gatling.simulation :as simulation]))

(def buffer-size 20000)

(defn create-dir [dir]
  (.mkdirs (java.io.File. dir)))

(defn- gatling-csv-writer [path idx result-lines]
  (let [csv (csv/write-csv result-lines :delimiter "\t" :end-of-line "\n")]
   (spit (str path "/simulation" idx ".log") csv)))

(defn run-simulation [scenarios concurrency & [options]]
 (let [start-time (LocalDateTime.)
       results-dir (or (:root options) "target/results")
       step-timeout (or (:timeout-in-ms options) 5000)
       result (simulation/run-scenarios {:runner (choose-runner scenarios concurrency options)
                                         :timeout step-timeout
                                         :context (:context options)}
                                        (weighted-scenarios (range concurrency) scenarios))]
   (create-dir (str results-dir "/input"))
   (report/create-result-lines start-time
                               buffer-size
                               result
                               (partial gatling-csv-writer (str results-dir "/input")))
   (chart/create-chart results-dir)
   (println (str "Open " results-dir "/index.html"))))
