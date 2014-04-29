(ns playground.core
  (:use [clojure.core.async :refer [go put! <! >! chan timeout]]
        [playground.serialosc :refer [track-devices]]))

;(osc-debug true)

(def available-devices (track-devices))
(go
 (while true
   (let [devices (<! available-devices)]
     (println :devices (first (vals devices))))))





;; (def mPath "/dev/tty.usbserial-m0000962")

;; ;(def m128 (find-monome mPath))

;; (def monome (monome/connect mPath))

;; (defn on-action
;;   [action x y state]
;;   (println action x y state))

;; (handlers/on-action monome on-action :test)

;; ;; (poly/light-led-on-sustain monome)

;; ;; (poly/toggle-all monome)

;; (led/led-on monome 0 0)




;; (monome/connected? monome)

;; (monome/disconnect monome)
