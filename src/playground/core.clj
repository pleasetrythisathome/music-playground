(ns playground.core
  (:require [clojure.core.async :refer [go put! <! >! chan timeout]]
            [clojure.pprint :refer [pprint]]
            [playground.serialosc :refer [monitor-devices]]))

;(osc-debug true)

(def available-devices (monitor-devices))
(go
 (while true
   (let [devices (<! available-devices)]
     (pprint (first (vals devices))))))
