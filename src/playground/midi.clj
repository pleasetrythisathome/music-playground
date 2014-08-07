(ns playground.midi
  (:require [clojure.core.async
             :refer [<! >!  chan timeout go go-loop map< filter< close! take! put! alt!]
             :as async]
            [clojure.pprint :refer [pprint]])
  (:use [overtone.live]
        [monome-osc.core]
        [playground.utils]
        [playground.arc]
        [playground.monome]))

(pprint (midi-connected-devices))
