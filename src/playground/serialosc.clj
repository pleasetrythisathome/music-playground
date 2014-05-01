(ns playground.serialosc
  (:require [clojure.pprint :refer [pprint]]
            [clojure.core.async :refer [go put! <! >! chan timeout]])
  (:use [overtone.osc]))

;(osc-debug true)

(def PORTS {:serialosc 12002
            :server 12001})
(def host "localhost")

(def server (osc-server (:server PORTS)))
(def to-serialosc (osc-client host (:serialosc PORTS)))

;(osc-close server)

(def devices (atom {}))

(defn listen-disconnect
  []
  (osc-send to-serialosc "/serialosc/notify" host (:server PORTS)))

(defn bind-handlers
  [chan handlers]
  (doall (map (fn [[action path]] (osc-handle server path #(put! chan [action (:args %)]))) handlers)))

(defn rm-handlers
  [handlers]
  (doall (map (fn [[action path]] (osc-rm-handler server path)) handlers)))

(defn monitor-devices
  []
  (let [connection (chan)
        handlers [[:add "/serialosc/device"]
                  [:add "/serialosc/add"]
                  [:remove "/serialosc/remove"]]
        out (chan)]
    (reset! devices {})

    (add-watch devices :change (fn [key ref old new] (when-not (= old new)
                                                      (put! out new))))

    (rm-handlers handlers)
    (bind-handlers connection handlers)

    (osc-send to-serialosc "/serialosc/list" host (:server PORTS))

    (go
     (while true
       (let [[action [id type port]] (<! connection)
             device {:id (keyword id)
                     :type type
                     :port port
                     :prefix (str "/" id)
                     :client (osc-client host port)}]
         (case action
           :add (swap! devices #(assoc % (:id device) device))
           :remove (swap! devices #(dissoc % (:id device))))
         (listen-disconnect))))
    out))

(defn get-devices
  []
  @devices)

(defn connect
  [{:keys [client prefix] :as device}]
  (osc-send client "/sys/port" (:server PORTS))
  (osc-send client "/sys/prefix" prefix))

(defn send-to [{:keys [client] :as device} path & args]
  (connect device)
  (apply (partial osc-send client (str (:prefix device) path)) args))


;; grid actions

;; (defn toggle-all
;;   [state]
;;   (osc-send client "/grid/led/all" state))
