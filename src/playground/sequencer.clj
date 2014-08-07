(ns playground.sequencer
  (:require [clojure.core.async
             :refer [<! <!! >! >!!  chan timeout go go-loop close! put! alts!]
             :as async]
            [clojure.pprint :refer [pprint]])
  (:use [overtone.live]
        [playground.utils]))

(defrecord Note [pitch duration velocity])
(defn make-note
  "creates an instance of a midi note"
  [& {:keys [pitch
             duration
             velocity]
      :or {pitch 400
           duration 1
           velocity 8}}]
  (Note. pitch duration velocity))

(defprotocol NoteSequence
  (add-note [this index note]))

(defprotocol Loopable
  (play-loop [this nome fn]))

(defrecord Sequence [notes]
  NoteSequence
  (add-note [this index note]
    (update-in this [:notes index] conj note))
  Loopable
  (play-loop [this nome f]
    (let [beat (nome)
          index (-> beat
                    (- 1)
                    (mod (count (:notes this))))]
      (apply-at (nome beat) f (get-in this [:notes index]) [])
      (apply-by (nome (inc beat)) play-loop this nome f []))))

(defn make-sequence
  "creates an instance of an empty sequence"
  [& {:keys [length]
      :or {length 8}}]
  (Sequence. (into [] (repeat length []))))

(defprotocol Sequencer
  (add-sequence [this s])
  (reset [this]))

(defrecord StepSequencer [sequences]
  Sequencer
  (add-sequence [this s]
    (update-in this [:sequences] conj s))
  (reset [this]
    (assoc this :sequences [])))

(def sequencer (atom (StepSequencer. [])))

(comment
  (let [s (add-note (make-sequence) 0 (make-note))]
    (swap! sequencer add-sequence s)
    (play-loop (get-in @sequencer [:sequences 0]) (metronome 120) log-m))

  (pprint @sequencer)
  (swap! sequencer reset)
  )
