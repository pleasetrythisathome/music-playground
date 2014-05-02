(ns playground.serialosc
  (:require [clojure.pprint :refer [pprint]]
            [clojure.core.async :refer [go put! <! >! chan timeout]])
  (:use [overtone.osc]))

;(osc-debug true)

(defonce PORTS {:serialosc 12002
            :server 12001})
(defonce host "localhost")

(defonce server (osc-server (:server PORTS)))
(defonce to-serialosc (osc-client host (:serialosc PORTS)))

;(osc-close server)

(defonce devices (atom {}))
(defonce clients (atom {}))

(defn listen-disconnect
  []
  (osc-send to-serialosc "/serialosc/notify" host (:server PORTS)))

(defn bind-handlers
  [chan handlers]
  (doall (map (fn [[action path]] (osc-handle server path #(put! chan [action (:args %)]))) handlers)))

(defn rm-handlers
  [handlers]
  (doall (map (fn [[action path]] (osc-rm-handler server path)) handlers)))

(defn get-devices
  []
  @devices)

(defn get-client
  [{:keys [id] :as device}]
  (get @clients id))

(defn send-to [{:keys [prefix] :as device} path & args]
  (let [client (get-client device)]
    (apply (partial osc-send client (str prefix path)) args)))

(defn set-prefix
  [device prefix]
  (let [client (get-client device)]
    (osc-send client "/sys/prefix" prefix)))

;; grid actions

;;leds

(defn row->bitmask
  [row]
  (Integer/parseInt (apply str row) 2))

(defn set-led
  [monome x y state]
  (send-to monome "/grid/led/set" x y state))

(defn set-all
  [monome state]
  (send-to monome "/grid/led/all" state))

(defn set-quad
  "state is a [] size 8 where [[row] ...]"
  [monome x-off y-off state]
  (send-to monome "/grid/led/map" x-off y-off (map row->bitmask state)))

(defn set-row
  "x-off must be a multiple of 8"
  [monome x-off y state]
  (send-to monome "/grid/led/row" x-off y (row->bitmask state)))

(defn set-column
  "x-off must be a multiple of 8"
  [monome x y-off state]
  (send-to monome "/grid/led/col" x y-off (row->bitmask state)))

;; animations

(defn connect-animation
  [monome]
  (let [row-on (apply vector (repeat 10 1))
        row-off (apply vector (repeat 10 0))]
    (go
     (loop [col 0]
       (set-column monome col 0 row-on)
       (<! (timeout 25))
       (set-column monome col 0 row-off)
       (when (< col 18)
         (recur (inc col)))))))

;; add brightness operations

;; buttons

(defn monome-listen
  [monome]
  (let [out (chan)
        prefix (:prefix monome)
        handlers [[:button (str prefix "/grid/key")]
                  [:tilt (str prefix "/tilt")]]]
    (bind-handlers out handlers)
    out))

;; connection

(defn connect
  [{:keys [id port prefix] :as device}]
  (let [client (osc-client host port)]
    (swap! clients #(assoc % id client))
    (swap! devices #(assoc % id device))
    (osc-send client "/sys/port" (:server PORTS))
    (set-prefix device prefix)
    (connect-animation device)))

(defn disconnect
  [{:keys [id] :as device}]
  (let [client (get-client device)]
    (osc-close client)
    (swap! clients #(dissoc % id))
    (swap! devices #(dissoc % id))))

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
                     :prefix (str "/" id)}]
         (case action
           :add (connect device)
           :remove (disconnect device))
         (listen-disconnect))))
    out))
