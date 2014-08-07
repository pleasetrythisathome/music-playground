(ns playground.arc
  (:require [clojure.core.async
             :refer [<! >! chan timeout go go-loop map< filter< close!]
             :as async])
  (:use [monome-osc.core]
        [playground.utils]))

(def encoders (atom []))
(defn reset-encoders! []
  (reset! encoders (into [] (repeat 4 {:step 1
                                       :scale 64
                                       :value 0
                                       :min 0
                                       :max 1000}))))

(defn get-enc-value [n]
  (get-in @encoders [n :value]))

(defn update-encoder [n f]
  (swap! encoders (fn [all] (update-in all [n] f))))
(defn set-encoder [n {:keys [step scale value] :as v}]
  (update-encoder n #(merge % v)))

(defn scaled-value [arc n {:keys [scale value]}]
  (let [led (-> value
                (/ scale)
                (* 64)
                (mod 64)
                (int))]
    (set-all arc n 0)
    (set-led arc n led 15)))

(defn map-range [x start end new-start new-end]
  (+ new-start (* (- new-end new-start) (if (zero? x)
                                          start
                                          (/ (- x start) (- end start))))))
(defn constrain [x min max]
  (if (< x max)
    (if (< min x)
      x
      min)
    max))
(defn enc-dial [arc n {:keys [value min max]}]
  (let [end (int (map-range value min max 0 64))]
    (set-range arc n 0 end 15)
    (when (< end 62)
        (set-range arc n (inc end) 63 0))))

(defn on-change [arc f k r os ns]
  (doseq [n (range 4)]
    (let [old (get os n)
          new (get ns n)]
      (when-not (= old new)
        (f arc n new)))))

(defn update-on-delta [arc]
  (let [delta (map< second (filter< #(= :delta (first %)) (listen-to arc)))
        control (chan)]
    (go-loop []
             (when-let [[n d] (first (alts! [delta control]))]
               (update-encoder n (fn [{:keys [step value min max] :as enc}]
                                   (assoc enc :value (-> d
                                                         (* step)
                                                         (+ value)
                                                         (constrain min max)))))
               (recur)))
    control))
