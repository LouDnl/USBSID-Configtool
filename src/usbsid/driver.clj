(ns usbsid.driver
  "Will drive you to the chiptune garden!"
  (:require
   [clojure.string :as string]
   [usbsid.logging :as logging]
   [usbsid.state :as state]
   [usbsid.config-model :as model])
  (:import
   [usbsid USBSID USBSIDDevice Config$Cfg Config$CLK Cmd]))


(defonce ^:private drv-atom (atom (USBSID.)))


;;; Byte helpers

(defn- b->int [b] (Byte/toUnsignedInt b))

(defn- ba-get [^bytes arr i] (b->int (aget arr i)))

(defn- cfg-byte [v] (byte (bit-and v 0xff)))

(defn- sid-addr [id]
  (case (int id) 0 0x00, 1 0x20, 2 0x40, 3 0x60, 0xFF))


;;; Parsers

(defn parse-config-bytes
  "Parses the config byte-buffer into a Clojure datastructure"
  [^bytes buf]
  (let [g         (fn [i] (ba-get buf i)) ; get byte from buffer
        clock-val (bit-or (bit-shift-left (g 7) 16)
                          (bit-shift-left (g 8) 8)
                          (g 9))
        clk-enum  (Config$CLK/getCLK (int clock-val))
        clk-id    (if clk-enum (Config$CLK/clkID clk-enum) 1)
        s1-id1    (bit-and (g 13) 0xF)
        s1-id2    (bit-shift-right (bit-and (g 13) 0xF0) 4)
        s2-id1    (bit-and (g 24) 0xF)
        s2-id2    (bit-shift-right (bit-and (g 24) 0xF0) 4)]
    {:need-confirmation    (pos? (g 2)) ; Need configuration confirmation v1.5+ only
     :disable-changedetect (pos? (g 3)) ; Disable socket change detection v1.5+ only
     ; Clockworx
     :lock-clockrate       (pos? (g 5))
     :external-clock       (pos? (g 6))
     :clock-rate           (:key (get model/clock-rate-by-id clk-id {:key :pal}))
     ; SocketOne
     :socket-one           {:enabled  (pos? (g 10))
                            :dualsid  (pos? (g 11))
                            :chiptype (:key (get model/chip-type-by-id (g 12) {:key :unknown}))
                            :sid1     {:id   s1-id1
                                       :addr (sid-addr s1-id1)
                                       :type (:key (get model/sid-type-by-id (g 14) {:key :unknown}))}
                            :sid2     {:id   s1-id2
                                       :addr (sid-addr s1-id2)
                                       :type (:key (get model/sid-type-by-id (g 15) {:key :unknown}))}}
     ; SocketTwo
     :socket-two           {:enabled  (pos? (g 20))
                            :dualsid  (pos? (g 21))
                            :chiptype (:key (get model/chip-type-by-id (g 23) {:key :unknown}))
                            :sid1     {:id   s2-id1
                                       :addr (sid-addr s2-id1)
                                       :type (:key (get model/sid-type-by-id (g 25) {:key :unknown}))}
                            :sid2     {:id   s2-id2
                                       :addr (sid-addr s2-id2)
                                       :type (:key (get model/sid-type-by-id (g 26) {:key :unknown}))}}
     ; Bright light, bright light!!
     :led                  {:enabled      (pos? (g 30))
                            :idle-breathe (pos? (g 31))}
     ; I can see colors!
     :rgbled               {:enabled      (pos? (g 40))
                            :idle-breathe (pos? (g 41))
                            :brightness   (g 42)
                            :sid-to-use   (let [v (g 43)] (if (zero? v) -1 v))}
     ; Stuffs
     :cdc                  {:enabled (pos? (g 51))}
     :webusb               {:enabled (pos? (g 52))}
     :asid                 {:enabled (pos? (g 53))}
     :midi                 {:enabled (pos? (g 54))}
     ; Stuff for keyboard people
     :fmopl                {:enabled (pos? (g 55))
                            :sidno   (g 56)}
     ; Headphonez on!
     :stereo-en            (pos? (g 57))
     :lock-audio-sw        (pos? (g 58))
     :mirrored             (pos? (bit-and (g 60) 0x1))
     :flipped              (pos? (bit-and (g 60) 0x2))
     :mixed                (pos? (bit-and (g 60) 0x4))}))


;;; Write config helpers

(defn config->commands
  "Converts a config map to seq of [section item value] triples for SET_CONFIG.
   Testable without hardware."
  [cfg]
  (let [s1  (:socket-one cfg)
        s2  (:socket-two cfg)
        led (:led cfg)
        rgb (:rgbled cfg)]
    [[0x0 (:id (get model/clock-rate-by-key (:clock-rate cfg) {:id 1}))
      (if (:lock-clockrate cfg) 1 0)]
     [0x1 0x0 (if (:enabled s1) 1 0)]
     [0x1 0x1 (if (:dualsid s1) 1 0)]
     [0x1 0x2 (:id (get model/chip-type-by-key (:chiptype s1) {:id 1}))]
     [0x1 0x4 (:id (get model/sid-type-by-key (get-in s1 [:sid1 :type]) {:id 0}))]
     [0x1 0x5 (:id (get model/sid-type-by-key (get-in s1 [:sid2 :type]) {:id 0}))]
     [0x2 0x0 (if (:enabled s2) 1 0)]
     [0x2 0x1 (if (:dualsid s2) 1 0)]
     [0x2 0x2 (:id (get model/chip-type-by-key (:chiptype s2) {:id 1}))]
     [0x2 0x4 (:id (get model/sid-type-by-key (get-in s2 [:sid1 :type]) {:id 0}))]
     [0x2 0x5 (:id (get model/sid-type-by-key (get-in s2 [:sid2 :type]) {:id 0}))]
     [0x3 0x0 (if (:enabled led) 1 0)]
     [0x3 0x1 (if (:idle-breathe led) 1 0)]
     [0x4 0x0 (if (:enabled rgb) 1 0)]
     [0x4 0x1 (if (:idle-breathe rgb) 1 0)]
     [0x4 0x2 (:brightness rgb)]
     [0x4 0x3 (max 0 (:sid-to-use rgb))]
     [0x9 (if (get-in cfg [:fmopl :enabled]) 1 0) 0]
     [0xA (if (:stereo-en cfg) 1 0) 0]
     [0xB (if (:lock-audio-sw cfg) 1 0) 0]
     [0xC (if (:mirrored cfg) 1 0) 0]
     [0xD (if (:flipped cfg) 1 0) 0]
     [0xE (if (:mixed cfg) 1 0) 0]]))

(defn- set-cfg
  "Wrapper around .USBSID_sendconfigcommand"
  [& {:keys [a b c d]
      :or {a 0 b 0 c 0 d 0}}]
  (.USBSID_sendconfigcommand
   @drv-atom
   (bit-and (.get Config$Cfg/SET_CONFIG) 0xff)
   (into-array Byte [(cfg-byte a) (cfg-byte b) (cfg-byte c) (cfg-byte d)])))

(defn send-config!
  "Write all settings from the supplied config, 1 write per setting"
  [cfg]
  (doall
   (doseq [[a b c] (config->commands cfg)]
     (try
       (set-cfg {:a a :b b :c c})
       (catch Exception e
         (state/log! (format "send-config! error [0x%X 0x%X 0x%X]: %s" a b c (.getMessage e)))
         (throw e))))))


;;; Internal driver API

(defn- with-driver
  "Exception catch wrapper around driver functions"
  [label f]
  (future
    (try
      (f)
      (catch Exception e
        (.error @logging/logger e)
        (state/log! (str label " error: " (.getMessage e)))))))

(defn- valid-fw-version?
  "FW version looks like '0.6.0-BETA.20250101', starts with 'digit', length > 3."
  [s]
  (and (string? s) (> (count s) 3) (string/includes? s ".") (not (string/includes? s "ERROR"))))

(defn- valid-pcb-version?
  "PCB version looks like '1.5', has a dot, length > 2."
  [s]
  (and (string? s) (> (count s) 2) (string/includes? s ".") (not (string/includes? s "ERROR"))))

(defn- read-with-retry
  "Reads can sometimes return garbage.
   Call read-fn up to max-attempts times, sleeping delay-ms between each,
   until valid-fn returns true. Returns last result regardless."
  [read-fn valid-fn max-attempts delay-ms]
  (loop [n max-attempts]
    (let [result (try (read-fn) (catch Exception _ nil))]
      (cond
        (valid-fn result) result
        (pos? n)          (do (Thread/sleep delay-ms) (recur (dec n)))
        :else             result))))

(defn- valid-config-response?
  "Firmware sends: result[0]=0x30 (initiator), result[1]=0x7F (verification), result[63]=0xFF (terminator)."
  [^bytes b]
  (and b
       (= (alength b) 64)
       (= (Byte/toUnsignedInt (aget b 0))  0x30)
       (= (Byte/toUnsignedInt (aget b 1))  0x7F)
       (= (Byte/toUnsignedInt (aget b 63)) 0xFF)))

(defn- do-read-config
  "Reads the complete configuration and returns a Byte array"
  []
  (.USBSID_rwconfigcommand
   @drv-atom
   (bit-and (.get Config$Cfg/READ_CONFIG) 0xff)
   (int 64)
   (into-array Byte [(byte 0)])))

(defn- do-read-pcbversion
  "Read, verify and return PCB version"
  []
  (let [rawread    (.USBSID_rwconfigcommand
                    @@#'usbsid.driver/drv-atom
                    (bit-and (.get usbsid.Config$Cfg/US_PCB_VERSION) 0xff)
                    (int 64)
                    (into-array Byte [(byte 0)]))
        rawversion (java.util.Arrays/copyOfRange rawread 2 (+ 2 (Byte/toUnsignedInt (aget rawread 1))))]
    (if (= (aget rawread 0) (.get usbsid.Config$Cfg/US_PCB_VERSION))
      (String. rawversion)
      "0.0-ERROR")))

(defn- do-read-fwversion
  "Read, verify and return FW version"
  []
  (let [rawread    (.USBSID_rwconfigcommand
                    @drv-atom
                    (bit-and (.get Config$Cfg/USBSID_VERSION) 0xff)
                    (int 64)
                    (into-array Byte [(byte 0)]))
        rawversion (java.util.Arrays/copyOfRange rawread 2 (+ 2 (Byte/toUnsignedInt (aget rawread 1))))]
    (if (= (aget rawread 0) (.get usbsid.Config$Cfg/USBSID_VERSION))
      (String. rawversion)
      "0.0.0-ERROR")))


;;; Public driver API

(defn connected? [] (USBSIDDevice/isOpen))

(defn read-config!
  "Read USBSID-Pico configuration"
  []
  (with-driver "Read config"
    (fn []
      (state/log! "Reading configuration...")
      (let [result (read-with-retry do-read-config valid-config-response? 3 50)]
        (if (valid-config-response? result)
          (let [cfg (parse-config-bytes result)]
            (swap! state/*state assoc :config cfg :dirty false)
            (swap! state/*state assoc-in [:connection :config-status] :loaded)
            (when (:need-confirmation cfg)
              (state/set-section! :sockets))
            (state/log! "Configuration loaded."))
          (state/log! "Read config: invalid response after retries"))))))

(defn connect!
  "Connect to USBSID-Pico"
  []
  (with-driver "Connect"
    (fn []
      (reset! drv-atom (USBSID.))
      (state/log! "Connecting to USBSID-Pico...")
      (let [result (.USBSID_init @drv-atom)]
        (if (zero? result)
          (let [fw  (read-with-retry #(do-read-pcbversion)  valid-fw-version?  3 50)
                pcb (read-with-retry #(do-read-fwversion) valid-pcb-version? 3 50)]
            (state/set-connection! :connected fw pcb)
            (state/log! (str "Connected! FW: v" fw "  PCB: v" pcb))
            @(read-config!))
          (do (state/set-connection! :disconnected nil nil)
              (state/log! "Connection failed - device not found")))))))

(defn disconnect!
  "Disconnect from USBSID-Pico"
  []
  (with-driver "Disconnect"
    (fn []
      (state/log! "Disconnecting...")
      ; Don't care for Exceptions on disconnect, stops flow otherwise
      (try (.USBSID_exit @drv-atom) (catch Exception _ nil))
      (state/set-connection! :disconnected nil nil) ; Will also set config-loaded to nothing
      (state/log! "Disconnected."))))

(defn write-config!
  "Write the current configuration to USBSID-Pico"
  []
  (with-driver "Write config"
    (fn []
      (state/log! "Writing configuration...")
      ; Change state _before_ writing the config in case of single thrown set_config exception
      (swap! state/*state assoc-in [:connection :config-status] :written)
      (send-config! (:config @state/*state))
      (state/log! "Configuration written."))))

(defn save-config!
  "Send a save to flash command to USBSID-Pico"
  []
  (with-driver "Save config"
    (fn []
      (state/log! "Saving configuration (no reboot)...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/SAVE_NORESET) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (swap! state/*state assoc :dirty false)
      (swap! state/*state assoc-in [:connection :config-status] :saved)
      (state/log! "Configuration saved."))))

(defn save-reboot!
  "Send a save to flash and reboot command to USBSID-Pico"
  []
  (with-driver "Save+reboot"
    (fn []
      (state/log! "Saving and rebooting board...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/SAVE_CONFIG) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (state/set-connection! :disconnected nil nil)
      (state/log! "Saved. Board rebooting - reconnect when ready."))))

(defn reset-config!
  "Send a reset config to default command to USBSID-Pico"
  []
  (with-driver "Reset config"
    (fn []
      (state/log! "Resetting to default configuration...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/RESET_CONFIG) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (state/log! "Reset to defaults.")
      (Thread/sleep 500) ; Sleep a while, stay forever?
      @(read-config!))))

(defn apply-config!
  "Send apply config command to USBSID-Pico"
  []
  (with-driver "Apply config"
    (fn []
      (state/log! "Applying configuration (no save)...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/APPLY_CONFIG) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (swap! state/*state assoc-in [:connection :config-status] :applied)
      (state/log! "Configuration applied."))))

(defn reload-config! []
  (with-driver "Reload config"
    (fn []
      (state/log! "Reloading configuration from flash...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/RELOAD_CONFIG) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (Thread/sleep 200)
      @(read-config!)
      (state/log! "Configuration reloaded from flash."))))

(defn confirm-config!
  "Send CONFIG_ACK (0xFA), enables socket power regulators on v1.5+ boards.
   Clears need_confirmation flag after ACK."
  []
  (with-driver "Confirm config"
    (fn []
      (state/log! "Sending CONFIG_ACK to board...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/CONFIG_ACK) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (state/set-config-value! [:need-confirmation] false)
      (swap! state/*state assoc-in [:connection :config-status] :confirmed)
      (state/log! "Configuration acknowledged. Socket power enabled."))))

(defn auto-detect!
  "Run AUTO_DETECT (0x5B): detects chip + SID types, saves, applies. Doesn't reboot!"
  []
  (with-driver "Auto detect"
    (fn []
      (state/log! "Starting auto-detection (chip + SID types)...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/AUTO_DETECT) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (Thread/sleep 3000)
      (state/log! "Auto-detection complete. Reading updated config...")
      @(read-config!))))

(defn detect-sids!
  "Send SID detect command to USBSID-Pico"
  []
  (with-driver "Detect SIDs"
    (fn []
      (state/log! "Detecting SID types (this may take a moment)...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/DETECT_SIDS) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (Thread/sleep 3000)
      (state/log! "Detection complete.")
      @(read-config!))))

(defn detect-clones!
  "Send Clone detect command to USBSID-Pico"
  []
  (with-driver "Detect clones"
    (fn []
      (state/log! "Detecting clone SID types...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/DETECT_CLONES) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (Thread/sleep 2000)
      (state/log! "Clone detection complete.")
      @(read-config!))))

(defn test-all-sids!
  "Send test all SIDs command to USBSID-Pico"
  []
  (with-driver "Test all SIDs"
    (fn []
      (state/log! "Running test on all SIDs (long)...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/TEST_ALLSIDS) 0xff)
       (into-array Byte [(cfg-byte 0)])))))

(defn test-sid!
  "Run test on SID n (1-4)."
  [n]
  (with-driver (str "Test SID " n)
    (fn []
      (state/log! (str "Running test on SID " n " (long)..."))
      (let [cmd (case n
                  1 Config$Cfg/TEST_SID1
                  2 Config$Cfg/TEST_SID2
                  3 Config$Cfg/TEST_SID3
                  4 Config$Cfg/TEST_SID4
                  Config$Cfg/TEST_SID1)]
        (.USBSID_sendconfigcommand
         @drv-atom
         (bit-and (.get cmd) 0xff)
         (into-array Byte [(cfg-byte 0)]))))))

(defn stop-tests!
  "Send stop tests command to USBSID-Pico"
  []
  (with-driver "Stop tests"
    (fn []
      (state/log! "Stopping all SID tests...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/STOP_TESTS) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (state/log! "Tests stopped."))))

(defn restart-bus!
  "Send restart bus command to USBSID-Pico"
  []
  (with-driver "Restart bus"
    (fn []
      (state/log! "Restarting DMA & PIO bus...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/RESTART_BUS) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (state/log! "Bus restarted."))))

(defn restart-clk!
  "Send restart clock command to USBSID-Pico"
  []
  (with-driver "Restart clock"
    (fn []
      (state/log! "Restarting PIO clocks...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/RESTART_BUS_CLK) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (state/log! "Clock restarted."))))

(defn sync-pios!
  "Send synchronise PIO's command to USBSID-Pico"
  []
  (with-driver "Sync PIOs"
    (fn []
      (state/log! "Syncing PIO clocks...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/SYNC_PIOS) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (state/log! "PIOs synced."))))

(defn reset-sids!
  "Send reset SIDs command to USBSID-Pico"
  []
  (with-driver "Reset SIDs"
    (fn []
      (state/log! "Resetting SID chips...")
      (.USBSID_reset @drv-atom (byte 0))
      (state/log! "SIDs reset."))))

(defn reset-mcu!
  "Send reboot command to USBSID-Pico"
  []
  (with-driver "Reset MCU"
    (fn []
      (state/log! "Resetting MCU (USB will disconnect)...")
      (.USBSID_sendconfigcommand
       @drv-atom
       (bit-and (.get Config$Cfg/RESET_USBSID) 0xff)
       (into-array Byte [(cfg-byte 0)]))
      (state/set-connection! :disconnected nil nil)
      (state/log! "MCU reset. Reconnect when ready."))))

(defn bootloader!
  "Send restart too bootloader command to USBSID-Pico"
  []
  (with-driver "Bootloader"
    (fn []
      (state/log! "Entering bootloader mode (USB will disconnect)...")
      (USBSIDDevice/sendCommand (.get Cmd/BOOTLOADER) (into-array Byte []))
      (state/set-connection! :disconnected nil nil)
      (state/log! "Board in bootloader mode. Flash new firmware, then reconnect."))))

(def ^:private preset->cfg
  "Maps preset key to [Config$Cfg cmd-byte arg-byte].
   SINGLE_SID   arg 0=socket1, 1=socket2 (firmware buffer[1]).
   MIRRORED_SID arg 0=single,  1=dual    (firmware buffer[1])."
  {:single-s1     [Config$Cfg/SINGLE_SID     0]
   :single-s2     [Config$Cfg/SINGLE_SID     1]
   :dual-s1       [Config$Cfg/DUAL_SOCKET1   0]
   :dual-s2       [Config$Cfg/DUAL_SOCKET2   0]
   :dual-both     [Config$Cfg/DUAL_SID       0]
   :triple-s1     [Config$Cfg/TRIPLE_SID     0]
   :triple-s2     [Config$Cfg/TRIPLE_SID_TWO 0]
   :quad          [Config$Cfg/QUAD_SID       0]
   :mirrored      [Config$Cfg/MIRRORED_SID   0]
   :mirrored-dual [Config$Cfg/MIRRORED_SID   1]
   :dual-flipped  [Config$Cfg/DUAL_FLIPPED   0]
   :quad-flipped  [Config$Cfg/QUAD_FLIPPED   0]
   :quad-mixed    [Config$Cfg/QUAD_MIXED     0]
   :quad-flipmix  [Config$Cfg/QUAD_FLIPMIX   0]})

(defn apply-preset!
  "Send direct preset command to board, then read config back."
  [preset-key]
  (with-driver "Apply preset"
    (fn []
      (if-let [[cmd arg] (get preset->cfg preset-key)]
        (do
          (state/log! (str "Applying preset: " (name preset-key) "..."))
          (.USBSID_sendconfigcommand @drv-atom
                                     (bit-and (.get cmd) 0xff)
                                     (into-array Byte [(cfg-byte arg)]))
          (Thread/sleep 100)
          (state/log! "Preset applied. Reading updated config...")
          @(read-config!))
        (state/log! (str "Preset " (name preset-key) " has no direct command"))))))
