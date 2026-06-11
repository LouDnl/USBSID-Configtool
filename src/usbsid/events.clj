(ns usbsid.events
  "It's a party up in here!"
  (:require
   [usbsid.state :as state]
   [usbsid.driver :as driver]
   [usbsid.ini-io :as ini-io])
  (:import
   [javafx.stage FileChooser FileChooser$ExtensionFilter]))

(defmulti handle :event/type)

(defmethod handle :default [event]
  (println "Unhandled event:" event))

(defmethod handle :navigate [{:keys [section]}]
  (state/set-section! section))

(defmethod handle :config-changed [{:keys [path value]}]
  (state/set-config-value! path value))

(defmethod handle :connect [_]
  (driver/connect!))

(defmethod handle :disconnect [_]
  (driver/disconnect!))

(defmethod handle :read-config [_]
  (driver/read-config!))

(defmethod handle :write-config [_]
  (driver/write-config!))

(defmethod handle :save-config [_]
  (driver/save-reboot!))

(defmethod handle :reset-config [_]
  (driver/reset-config!))

(defmethod handle :detect-sids [_]
  (driver/detect-sids!))

(defmethod handle :apply-preset [{:keys [preset]}]
  (if (driver/connected?)
    (driver/apply-preset! preset)
    (state/log!
     (str "Preset " (name preset) " selected (not connected - connect board to apply)"))))

(defmethod handle :apply-config [_]
  (driver/apply-config!))

(defmethod handle :save-noreset [_]
  (driver/save-config!))

(defmethod handle :apply-save-config [_]
  (do
    (driver/write-config!)
    (Thread/sleep 500) ; Sleep a while, stay forever?
    (driver/save-config!)
    (driver/apply-config!)
    (Thread/sleep 500) ; Sleep a while, stay forever?
    (driver/read-config!)))

(defmethod handle :reload-flash [_]
  (driver/reload-config!))

(defmethod handle :auto-detect [_]
  (driver/auto-detect!))

(defmethod handle :detect-clones [_]
  (driver/detect-clones!))

(defmethod handle :test-all-sids [_]
  (driver/test-all-sids!))

(defmethod handle :test-sid [{:keys [n]}]
  (driver/test-sid! n))

(defmethod handle :stop-tests [_]
  (driver/stop-tests!))

(defmethod handle :restart-bus [_]
  (driver/restart-bus!))

(defmethod handle :restart-clk [_]
  (driver/restart-clk!))

(defmethod handle :sync-pios [_]
  (driver/sync-pios!))

(defmethod handle :reset-sids [_]
  (driver/reset-sids!))

(defmethod handle :reset-mcu [_]
  (driver/reset-mcu!))

(defmethod handle :bootloader [_]
  (driver/bootloader!))

(defmethod handle :popup-show [{:keys [key]}]
  (swap! state/*state assoc :hover-popup key))

(defmethod handle :popup-hide [{:keys [key]}]
  (swap! state/*state
         (fn [s]
           (if (= (:hover-popup s) key)
             (assoc s :hover-popup nil)
             s))))

(defmethod handle :open-url [{:keys [url]}]
  (let [desktop (java.awt.Desktop/getDesktop)]
    (when (.isSupported desktop java.awt.Desktop$Action/BROWSE)
      (.browse desktop (java.net.URI. url)))))

(defmethod handle :confirm-config [_]
  (driver/confirm-config!))

(defn- ini-ext-filter []
  (FileChooser$ExtensionFilter. "INI files" ^"[Ljava.lang.String;" (into-array String ["*.ini"])))

(defmethod handle :export-ini [_]
  (let [s    @state/*state
        fc   (doto (FileChooser.)
               (.setTitle "Save Configuration as INI")
               (.setInitialFileName "USBSID-Pico-cfg.ini")
               (-> .getExtensionFilters (.add (ini-ext-filter))))
        file (.showSaveDialog fc nil)]
    (when file
      (let [ini (ini-io/config->ini
                 (:config s)
                 (get-in s [:connection :fw-version]))]
        (spit file ini)
        (swap! state/*state assoc-in [:connection :config-status] :exportini)
        (state/log! (str "Config exported to " (.getName file)))))))

(defmethod handle :import-ini [_]
  (let [fc   (doto (FileChooser.)
               (.setTitle "Load Configuration from INI")
               (-> .getExtensionFilters (.add (ini-ext-filter))))
        file (.showOpenDialog fc nil)]
    (when file
      (let [ini-str  (slurp file)
            base-cfg (:config @state/*state)
            new-cfg  (ini-io/ini->config ini-str base-cfg)]
        (swap! state/*state
               #(-> %
                    (assoc :config new-cfg :dirty true)
                    (assoc-in [:connection :config-status] :importini)))
        (state/log! (str "Config imported from " (.getName file)))))))
