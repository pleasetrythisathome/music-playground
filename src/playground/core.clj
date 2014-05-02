(ns playground.core
  (:require [clojure.core.async :refer [go put! <! >! chan timeout]]
            [clojure.pprint :refer [pprint]])
  (:use [playground.serialosc]))

;(osc-debug true)

;; tests

;; (def connected-devices (monitor-devices))
;; (def monome (first (vals (get-devices))))
;; (pprint monome)
;; (set-all monome 1)
;; (set-all monome 0)


;; (set-led monome 0 0 1)
;; (set-led monome 0 0 0)

;; (def row-on (apply vector (repeat 10 1)))
;; (def row-off (apply vector (repeat 10 0)))
;; (set-row monome 0 0 row-on)
;; (set-row monome 0 0 row-off)

;; (set-column monome 0 0 row-on)
;; (set-column monome 0 0 row-off)

;; (connect-animation monome)

#_(defn handle-event
  [[action args]]
  (case action
    :button (pprint args)
    :tilt nil))

#_(let [events (monome-listen monome)]
    (go
     (while true
       (let [event (<! events)]
         (handle-event event)))))

;; monitor devices

#_(go
   (while true
     (let [devices (<! connected-devices)]
       (if-let [monome (first (vals devices))]
         (print :disconnected)))))
