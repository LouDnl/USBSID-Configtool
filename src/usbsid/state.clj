(ns usbsid.state
  "Fluid stuff"
  (:require
   [clojure.java.io :as io]
   [clojure.string :refer [trim-newline]]
   [usbsid.config-model :as model]
   [usbsid.logging :refer [logger]]))


(def initial-state
  {:connection     {:status        :disconnected
                    :fw-version    nil
                    :pcb-version   nil
                    :config-status :none}
   :config         model/initial-config
   :active-section :about
   :log            [(format
                     "USBSID-Pico Config Tool v%s"
                     (trim-newline (slurp (io/resource ".version"))))]
   :dirty          false})

(def *state (atom initial-state))

(defn log!
  "Logging wrapper"
  [msg]
  (swap! *state update :log conj msg)
  (.info @logger msg))

(defn set-section!
  "Active section changer"
  [section]
  (swap! *state assoc :active-section section))

(defn set-config-value!
  "Change a config value"
  [path value]
  (swap! *state
         #(-> %
              (assoc-in (into [:config] path) value)
              (assoc :dirty true))))

(defn set-connection!
  "Update connection status"
  [status fw pcb]
  (swap! *state
         assoc :connection
         {:status      status
          :fw-version  fw
          :pcb-version pcb}))
