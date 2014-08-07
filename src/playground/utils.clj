(ns playground.utils
  (:require [clojure.core.async :refer [<!! >!! thread chan go-loop <!]]
            [clojure.pprint :refer [pprint]]))

(def log-chan (chan))

(thread
 (loop []
   (when-let [v (<!! log-chan)]
     (pprint v)
     (recur)))
 (println "Log Closed"))

;; (close! log-chan)

(defn log-m [& msgs]
  (doseq [msg msgs]
    (>!! log-chan (or msg "**nil**"))))

(defn log-loop [in]
  (go-loop []
           (when-let [e (<! in)]
             (log-m e)
             (recur)))
  in)
