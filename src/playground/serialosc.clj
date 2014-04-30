(ns playground.serialosc
  (:use [overtone.osc]
        [clojure.core.async :refer [go put! <! >! chan timeout]]))

(defonce PORTS {:serialosc 12002
                :server 4242})
(defonce host "127.0.0.1")

(defonce server (osc-server (:server PORTS)))
(defonce client (osc-client "localhost" (:serialosc PORTS)))

(defonce devices (atom {}))

(defn listen-disconnect
  []
  (osc-send client "/serialosc/notify" host (:server PORTS)))

(defn bind-handlers
  [server chan handlers]
  (doall (map (fn [[action path]] (osc-handle server path #(put! chan [action (:args %)]))) handlers)))

(defn rm-handlers
  [server handlers]
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

    (rm-handlers server handlers)
    (bind-handlers server connection handlers)

    (osc-send client "/serialosc/list" host (:server PORTS))

    (go
     (while true
       (let [[action [id type port]] (<! connection)
             device {:id (keyword id)
                     :type type
                     :port port}]
         (case action
           :add (swap! devices #(assoc % (:id device) device))
           :remove (swap! devices #(dissoc % (:id device))))
         (listen-disconnect))))
    out))

;; (defn connect
;;   [device]
;;   (print :connect device)
;;   (osc-send client "/serialosc/list" host (:port device)))

;; (defn toggle-all
;;   [state]
;;   (osc-send client "/grid/led/all" state))
