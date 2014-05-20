(ns playground.core
  (:require [clojure.core.async
             :refer [<! >!  chan timeout go go-loop map< filter< close! take! put! alt!]
             :as async]
            [clojure.pprint :refer [pprint]])
  (:use [overtone.live]
        [monome-osc.core]
        [playground.utils]
        [playground.arc]
        [playground.monome]))

(def monome (get-device :monome))
(def arc (get-device :arc))

;; init tests
(comment
  (log-m (:info monome))
  (log-m (:info arc))
  (set-all monome 1)
  (set-all arc 0 15)
  (reset-device monome)
  (reset-device arc)

  ;; (osc-listen server log-m :debug)
  ;; (osc-rm-listener server :debug)

  (def log-events (log-loop (sub-device monome :press))))
  (close! log-events)

;; synths


(definst sin-wave [freq 440 attack 0.01 sustain 0.4 release 0.1 vol 0.4]
  (* (env-gen (lin attack sustain release) 1 1 0 1 FREE)
     (sin-osc freq)
     vol))

(definst saw-wave [freq 440 attack 0.01 sustain 0.4 release 0.1 vol 0.4]
  (* (env-gen (lin attack sustain release) 1 1 0 1 FREE)
     (saw freq)
     vol))

(definst square-wave [freq 440 attack 0.01 sustain 0.4 release 0.1 vol 0.4]
  (* (env-gen (lin attack sustain release) 1 1 0 1 FREE)
     (lf-pulse:ar freq)
     vol))

(definst noisey [freq 440 attack 0.01 sustain 0.4 release 0.1 vol 0.4]
  (* (env-gen (lin attack sustain release) 1 1 0 1 FREE)
     (pink-noise) ; also have (white-noise) and others...
     vol))

(definst triangle-wave [freq 440 attack 0.01 sustain 0.1 release 0.4 vol 0.4]
  (* (env-gen (lin attack sustain release) 1 1 0 1 FREE)
     (lf-tri freq)
     vol))

(definst spooky-house [freq 440 width 0.2
                         attack 0.3 sustain 4 release 0.3
                         vol 0.4]
  (* (env-gen (lin attack sustain release) 1 1 0 1 FREE)
     (sin-osc (+ freq (* 20 (lf-pulse:kr 0.5 0 width))))
     vol))

(sin-wave)
(saw-wave)
(square-wave)
(noisey)
(triangle-wave)

;; controls

(defn control [synth key v]
  (ctl synth key v))

(defn bind-encoders [synth & mapping]
  (let [n-key (apply hash-map mapping)
        tag (keyword (:name synth))]
    (add-watch encoders tag (fn [k r os ns]
                                        (doseq [n (vals n-key)
                                                key (keys n-key)]
                                          (let [value (get-in ns [n :value])]
                                            (control synth key value)))))))

(defn release-encoders [synth]
  (let [tag (keyword (:name synth))]
    (remove-watch encoders tag)))

(defn map-kv [f coll]
  (reduce-kv (fn [out k v] (assoc out k (f k v))) {} coll))

(defn bind-trigger [synth key & mappings]
  (let [presses (filter< #(= key %) (sub-device monome :press))]
    (go-loop []
             (when-let [press (first (alts! [presses control]))]
               (let [params (->> mappings
                                 (partition 2)
                                 (mapcat (fn [[k n]]
                                           [k (get-in @encoders [n :value])])))]
                 (log-m params)
                 (apply synth params)
                 (recur))))
    control))

;; (reset-encoders!)
;; (add-watch encoders :mirror (partial on-change arc scaled-value))
(add-watch encoders :mirror (partial on-change arc enc-dial))
;; (remove-watch encoders :mirror)


(def delta (update-on-delta arc))
;; (close! delta)

(log-m @encoders)

;; live

(bind-encoders quux :freq0 )
(release-encoders quux)

(def trigger (bind-trigger sin-wave [0 0] :freq 0 :attack 1 :sustain 2 :release 3))
;; (close! trigger)


(set-encoder 0 {:value 440
                :min 0
                :max 1000})
(set-encoder 1 {:value 0.01
                :step 0.001
                :min 0
                :max 0.1})
(set-encoder 2 {:value 0.4
                :step 0.01
                :min 0
                :max 2})
(set-encoder 3 {:value 0.1
                :step 0.01
                :min 0
                :max 1})

(stop)
