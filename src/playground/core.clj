(ns playground.core
  (:require [clojure.core.async
             :refer [<! >!  chan timeout go go-loop map< filter< close! take! put! alt!]
             :as async])
  (:use [overtone.live]
        [monome-osc.core]
        [playground.utils]
        [playground.arc]
        [playground.monome]))

;; these autodetect and get the device instances
(def monome (get-device :monome))
(def arc (get-device :arc))


;; init tests
(comment
  ;; you can
  (log-m (:info monome))
  (log-m (:info arc))
  ;; to see info about them in the repl
  ;; log-m is a utility that logs into the repl in the main context that prints to the repl. allows for printing from inside other threads and go blocks

  ;; make lights!
  (set-all monome 1)
  (set-all arc 0 15)
  (reset-device monome)
  (reset-device arc)

  ;; (osc-listen server log-m :debug)
  ;; (osc-rm-listener server :debug)

  ;; log all events from monome
  (def log-events (log-loop (sub-events monome)))
  ;; log-loop is a utility function that takes a channel, logs everything that comes out of it, and returns a channel that kills the whole bit when you close it
  (close! log-events)

  ;; log just the key of presses only
  (def presses (->> monome
                    sub-events
                    (filter< (fn [[key & args]]
                               (= key :press)))
                    (map< second)
                    log-loop))
  ;; stop
  (close! presses)


  )

;; timing

;; i'm not sure this metronome stuff works the way i intended. see sequencer.clj for more, and probably better
(defn my-metronome [bpm per-measure subdivisions]
  (let [out (chan (async/dropping-buffer 1))
        wait (/ 60000 (* bpm subdivisions))]
    (go-loop [beat 0
              sub 0]
      (>! out [beat sub])
      (<! (timeout wait))
      (recur (mod (inc beat) per-measure) (mod (inc sub) subdivisions)))
    out))

(def count (log-loop (my-metronome 60 2 3)))
(close! count)

;; synths

;; stop stops all sounds
(stop)

;; some boring synthes to play with
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

(definst triangle-wave [freq 440 attack 0.01 sustain 0.1 release 0.4 vol 0.4]
  (* (env-gen (lin attack sustain release) 1 1 0 1 FREE)
     (lf-tri freq)
     vol))

(definst noisey [freq 440 attack 0.01 sustain 0.4 release 0.1 vol 0.4]
  (* (env-gen (lin attack sustain release) 1 1 0 1 FREE)
     (pink-noise) ;; also have (white-noise) and others...
     vol))

;; controls

(defn bind-encoders
  "binds encoders to control synth values. see below for examples"
  [synth & mapping]
  (let [n-key (apply hash-map mapping)
        tag (keyword (:name synth))]
    (add-watch encoders tag (fn [k r os ns]
                                        (doseq [n (vals n-key)
                                                key (keys n-key)]
                                          (let [value (get-in ns [n :value])]
                                            (ctl synth key value)))))))

(defn release-encoders
  "releases encoders from a synth"
  [synth]
  (let [tag (keyword (:name synth))]
    (remove-watch encoders tag)))

(defn map-kv
  "map over a hash-map and call (f k v)

  i first wrote this like
  (reduce-kv (fn [out k v] (assoc out k (f k v))) {} coll)

  the for version is way cooler"
  [f coll]
  (into {} (for [[k v] coll] [k (f k v)])))

(defn bind-trigger
  "bind a monome key to trigger a synth. also supply mappings to take params from encoders at trigger time.

  returns a control channel that will stop the triggering

  (bind-trigger sin-wave [0 0] :freq 0 :attack 1 :sustain 2 :release 3)
  will trigger sin-wave when [0 0] is pressed and take :freq from encoder 0, :attack from encoder 1..."
  [synth key & mappings]
  (let [presses (filter< #(= key %) (sub-events monome :press))
        control (chan)]
    (go-loop []
             (when-let [press (first (alts! [presses control]))]
               (let [params (->> mappings
                                 (partition 2)
                                 (mapcat (fn [[k n]]
                                           [k (get-enc-value n)])))]
                 (log-m params)
                 (apply synth params)
                 (recur))))
    control))

;; reset encoder values to the defaults in arc.clj
(reset-encoders!)
;; this one will show the value as a line that moves infinites as you spin relative to speed and whatnot
(add-watch encoders :mirror (partial on-change arc scaled-value))
;; this one is cooler. dials from 0-100%. very pretty and really addicting. i've never had so much fun moving dials that do absolutely nothing but make lights
(add-watch encoders :mirror (partial on-change arc enc-dial))
;; stop mirroring values on encoder leds
(remove-watch encoders :mirror)


(def delta (update-on-delta arc))
;; (close! delta)

(comment
  (log-m @encoders)

  ;; live

  (bind-encoders sin-wave :freq 0)
  (release-encoders sin-wave)

  (def trigger (bind-trigger sin-wave [0 0] :freq 0 :attack 1 :sustain 2 :release 3))
  ;; close
  (close! trigger)


  ;; you can set change encoder min, max, step (sensitivity), and value
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

  ;; i'm freaking out man!
  (stop)
)
