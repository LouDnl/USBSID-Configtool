(ns usbsid.config-model
  "USBSID-Pico config options")

(def clock-rates
  [{:key :default :label "DEFAULT (1.000 MHz)" :value 1000000 :id 0}
   {:key :pal     :label "PAL     (0.985 MHz)" :value 985248  :id 1}
   {:key :ntsc    :label "NTSC    (1.023 MHz)" :value 1022727 :id 2}
   {:key :drean   :label "DREAN   (1.023 MHz)" :value 1023440 :id 3}
   {:key :ntsc2   :label "NTSC2   (1.023 MHz)" :value 1022730 :id 4}])

(def clock-rate-by-id
  (into {} (map (fn [c] [(:id c) c]) clock-rates)))

(def clock-rate-by-key
  (into {} (map (fn [c] [(:key c) c]) clock-rates)))

(def chip-types
  [{:key :real     :label "Real SID" :id 0}
   {:key :unknown  :label "Unknown"  :id 1}
   {:key :skpico   :label "SKPico"   :id 2}
   {:key :armsid   :label "ARMSID"   :id 3}
   {:key :arm2sid  :label "ARM2SID"  :id 4}
   {:key :fpgasid  :label "FPGASID"  :id 5}
   {:key :redipsid :label "RedipSID" :id 6}
   {:key :pdsid    :label "PDSID"    :id 7}
   {:key :backsid  :label "BackSID"  :id 8}
   {:key :sidemu   :label "SIDEmu"   :id 9}])

(def chip-type-by-id
  (into {} (map (fn [c] [(:id c) c]) chip-types)))

(def chip-type-by-key
  (into {} (map (fn [c] [(:key c) c]) chip-types)))

(def sid-types
  [{:key :unknown :label "Unknown" :id 0}
   {:key :na      :label "N/A"     :id 1}
   {:key :mos8580 :label "MOS8580" :id 2}
   {:key :mos6581 :label "MOS6581" :id 3}
   {:key :fmopl   :label "FMOpl"   :id 4}])

(def sid-type-by-id
  (into {} (map (fn [s] [(:id s) s]) sid-types)))

(def sid-type-by-key
  (into {} (map (fn [s] [(:key s) s]) sid-types)))

(def fmopl-sid-options
  [{:key 0   :label "Disabled"}
   {:key 1   :label "SID 1"}
   {:key 2   :label "SID 2"}
   {:key 3   :label "SID 3"}
   {:key 4   :label "SID 4"}
   {:key 255 :label "Disabled"}])

(def sid-to-use-options
  [{:key -1 :label "Off"}
   {:key  1 :label "SID 1"}
   {:key  2 :label "SID 2"}
   {:key  3 :label "SID 3"}
   {:key  4 :label "SID 4"}])

(def voltages
  [{:mos6581  "12v"}
   {:mos8580   "9v"}
   {:unknown   "9v"}
   {:skpico    "9v"}
   {:armsid    "9v"}
   {:arm2sid   "9v"}
   {:fpgasid   "9v"}
   {:redipsid  "9v"}
   {:pdsid     "9v"}
   {:backsid   "9v"}
   {:sidemu    "9v"}])

(def voltage-by-type
  (apply merge voltages))

(defn chipvoltage
  [chipkey sidkey]
  (or (if (= chipkey :real)
        (get voltage-by-type sidkey)
        (get voltage-by-type chipkey))
      "N/A"))

(def presets
  [{:key :single-s1     :label "SINGLE SID (Socket 1)"        :id  0}
   {:key :single-s2     :label "SINGLE SID (Socket 2)"        :id  1}
   {:key :dual-both     :label "DUAL SID (S1 + S2)"           :id  2}
   {:key :dual-s1       :label "DUAL SID (Socket 1 only)"     :id  3}
   {:key :dual-s2       :label "DUAL SID (Socket 2 only)"     :id  4}
   {:key :triple-s1     :label "TRIPLE (Dual S1 + Single S2)" :id  5}
   {:key :triple-s2     :label "TRIPLE (Single S1 + Dual S2)" :id  6}
   {:key :quad          :label "QUAD SID (4 chips)"           :id  7}
   {:key :mirrored      :label "MIRRORED (S2 = S1)"           :id  8}
   {:key :mirrored-dual :label "MIRRORED DUAL"                :id  9}
   {:key :dual-flipped  :label "DUAL FLIPPED"                 :id 10}
   {:key :quad-flipped  :label "QUAD FLIPPED"                 :id 11}
   {:key :quad-mixed    :label "QUAD MIXED"                   :id 12}
   {:key :quad-flipmix  :label "QUAD FLIP+MIX"                :id 13}])

(def preset-by-id
  (into {} (map (fn [p] [(:id p) p]) presets)))

(def preset-by-key
  (into {} (map (fn [p] [(:key p) p]) presets)))

(def initial-config
  {:clock-rate           :pal
   :socket-one           {:chiptype :unknown
                          :voltage  :unknown
                          :sid1     {:id 0    :addr 0x00 :type :unknown}
                          :sid2     {:id 0xFF :addr 0xFF :type :na}
                          :enabled  true
                          :dualsid  false}
   :socket-two           {:chiptype :unknown
                          :voltage  :unknown
                          :sid1     {:id 1    :addr 0x20 :type :unknown}
                          :sid2     {:id 0xFF :addr 0xFF :type :na}
                          :enabled  true
                          :dualsid  false}
   :led                  {:enabled true :idle-breathe false}
   :rgbled               {:enabled false :idle-breathe false :brightness 127 :sid-to-use -1}
   :cdc                  {:enabled true}
   :webusb               {:enabled true}
   :asid                 {:enabled true}
   :midi                 {:enabled true}
   :fmopl                {:sidno 0 :enabled false}
   :external-clock       false
   :lock-clockrate       false
   :stereo-en            false
   :lock-audio-sw        false
   :mirrored             false
   :flipped              false
   :mixed                false
   :need-confirmation    false
   :disable-changedetect false})
