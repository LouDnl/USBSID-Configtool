(ns usbsid.state
  "Fluid stuff"
  (:require
   [clojure.java.io :as io]
   [clojure.string :refer [trim-newline]]
   [usbsid.config-model :as model]
   [usbsid.logging :as logging]))


(defn fw-version->line
  "Map a firmware version string to a config-schema branch keyword.
   v0.5.x / v0.6.x firmware share one byte layout (`:legacy`).
   v0.7+ firmware uses the current layout (`:v0_7`).
   Anything unparsable assumes the newest schema (`:unknown`) since older
   boards are the minority. Returns one of #{:legacy :v0_7 :unknown}."
  [fw]
  (or (when (string? fw)
        (when-let [[_ maj min upd] (re-find #"^v?(\d+)\.(\d+)\.(\d+)" fw)]
          (let [M (Long/parseLong maj)   ; major
                m (Long/parseLong min)   ; minor
                _u (Long/parseLong upd)] ; update
            (cond
              (and (zero? M) (<= m 6))  :legacy ; 0.1.0 ~  0.6.4
              (or
               (and (zero? M) (>= m 7))
               (and (>= M 1) (>= m 7))) :v0_7 ; 0.7.0+
              :else                     :unknown)))) ; usually when not connected
      :unknown)) ; usually when not connected

(def initial-state
  {:connection     {:status        :disconnected
                    :fw-version    nil
                    :pcb-version   nil
                    :fw-line       :unknown
                    :config-status :none}
   :config         model/initial-config
   :active-section :welcome
   :log            [(format
                     "USBSID-Pico Config Tool v%s"
                     (trim-newline (slurp (io/resource ".version"))))]
   :dirty          false
   :hover-popup    nil})

(def *state (atom initial-state))

(defmacro log!
  "Logging wrapper. Appends msg to UI log buffer + emits INFO to JUL with the
   caller's namespace + source line preserved (via `logging/info!`).
   Macro form so the call-site's `*ns*` and `(:line (meta &form))` reach the
   LogRecord — a defn here would always show `usbsid.state` and line 56."
  [msg]
  `(do
     (swap! usbsid.state/*state update :log conj ~msg)
     (logging/info! ~(str *ns*) ~(:line (meta &form)) ~msg)))

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
         {:status        status
          :fw-version    fw
          :pcb-version   pcb
          :fw-line       (fw-version->line fw)
          :config-status :none}))
