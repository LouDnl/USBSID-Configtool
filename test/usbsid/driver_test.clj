(ns usbsid.driver-test
  (:require
   [clojure.test        :refer [deftest is testing]]
   [usbsid.driver       :as driver]
   [usbsid.config-model :as model]))

; ── Test helpers ──────────────────────────────────────────────────────────────

(defn- make-buf
  "64-byte zero array with specific [offset value] pairs set.
  Values are treated as unsigned bytes (0-255)."
  [& pairs]
  (let [buf (byte-array 64)]
    (doseq [[i v] (partition 2 pairs)]
      (aset buf (int i) (unchecked-byte (int v))))
    buf))

; PAL clock:     985248 = 0x0F08A0 → bytes 0x0F 0x08 0xA0
(def pal-bytes     [7 0x0F  8 0x08  9 0xA0])
; NTSC clock:   1022727 = 0x0F9B07 → bytes 0x0F 0x9B 0x07
(def ntsc-bytes    [7 0x0F  8 0x9B  9 0x07])
; DEFAULT clock: 1000000 = 0x0F4240 → bytes 0x0F 0x42 0x40
(def default-bytes [7 0x0F  8 0x42  9 0x40])

; ── parse-config-bytes tests ──────────────────────────────────────────────────

(deftest parse-clock-rate
  (testing "PAL clock"
    (let [cfg (driver/parse-config-bytes (apply make-buf pal-bytes))]
      (is (= :pal (:clock-rate cfg)))))
  (testing "NTSC clock"
    (let [cfg (driver/parse-config-bytes (apply make-buf ntsc-bytes))]
      (is (= :ntsc (:clock-rate cfg)))))
  (testing "DEFAULT clock"
    (let [cfg (driver/parse-config-bytes (apply make-buf default-bytes))]
      (is (= :default (:clock-rate cfg))))))

(deftest parse-booleans
  (let [buf (apply make-buf
              (concat pal-bytes
                [2 1    ; need_confirmation
                 3 1    ; disable_changedetect
                 5 1    ; lock_clockrate
                 6 1    ; external_clock
                 57 1   ; stereo_en
                 58 1   ; lock_audio_sw
                 60 7])) ; mirrored|flipped|mixed all set
        cfg (driver/parse-config-bytes buf)]
    (is (true?  (:need-confirmation cfg)))
    (is (true?  (:disable-changedetect cfg)))
    (is (true?  (:lock-clockrate cfg)))
    (is (true?  (:external-clock cfg)))
    (is (true?  (:stereo-en cfg)))
    (is (true?  (:lock-audio-sw cfg)))
    (is (true?  (:mirrored cfg)))
    (is (true?  (:flipped cfg)))
    (is (true?  (:mixed cfg)))))

(deftest parse-booleans-false
  (let [cfg (driver/parse-config-bytes (byte-array 64))]
    (is (false? (:need-confirmation cfg)))
    (is (false? (:disable-changedetect cfg)))
    (is (false? (:lock-clockrate cfg)))
    (is (false? (:external-clock cfg)))
    (is (false? (:stereo-en cfg)))
    (is (false? (:lock-audio-sw cfg)))
    (is (false? (:mirrored cfg)))
    (is (false? (:flipped cfg)))
    (is (false? (:mixed cfg)))))

(deftest parse-mirrored-bits
  (testing "mirrored only (bit 0)"
    (let [cfg (driver/parse-config-bytes (make-buf 60 1))]
      (is (true?  (:mirrored cfg)))
      (is (false? (:flipped cfg)))
      (is (false? (:mixed cfg)))))
  (testing "flipped only (bit 1)"
    (let [cfg (driver/parse-config-bytes (make-buf 60 2))]
      (is (false? (:mirrored cfg)))
      (is (true?  (:flipped cfg)))
      (is (false? (:mixed cfg)))))
  (testing "mixed only (bit 2)"
    (let [cfg (driver/parse-config-bytes (make-buf 60 4))]
      (is (false? (:mirrored cfg)))
      (is (false? (:flipped cfg)))
      (is (true?  (:mixed cfg))))))

(deftest parse-socket-one
  (let [buf (apply make-buf
              (concat pal-bytes
                [10 1      ; enabled
                 11 1      ; dualsid
                 12 2      ; chiptype = SKPICO
                 13 0x10   ; sid1.id=0 (lo nibble), sid2.id=1 (hi nibble)
                 14 2      ; sid1.type = MOS8580
                 15 3]))   ; sid2.type = MOS6581
        cfg (driver/parse-config-bytes buf)]
    (is (true?    (get-in cfg [:socket-one :enabled])))
    (is (true?    (get-in cfg [:socket-one :dualsid])))
    (is (= :skpico (get-in cfg [:socket-one :chiptype])))
    (is (= 0      (get-in cfg [:socket-one :sid1 :id])))
    (is (= 0x00   (get-in cfg [:socket-one :sid1 :addr])))
    (is (= :mos8580 (get-in cfg [:socket-one :sid1 :type])))
    (is (= 1      (get-in cfg [:socket-one :sid2 :id])))
    (is (= 0x20   (get-in cfg [:socket-one :sid2 :addr])))
    (is (= :mos6581 (get-in cfg [:socket-one :sid2 :type])))))

(deftest parse-socket-two
  (let [buf (apply make-buf
              (concat pal-bytes
                [20 1      ; enabled
                 21 0      ; dualsid=false
                 23 3      ; chiptype = ARMSID
                 24 0x21   ; sid1.id=1 (lo nibble), sid2.id=2 (hi nibble)
                 25 3      ; sid1.type = MOS6581
                 26 0]))   ; sid2.type = UNKNOWN
        cfg (driver/parse-config-bytes buf)]
    (is (true?     (get-in cfg [:socket-two :enabled])))
    (is (false?    (get-in cfg [:socket-two :dualsid])))
    (is (= :armsid (get-in cfg [:socket-two :chiptype])))
    (is (= 1       (get-in cfg [:socket-two :sid1 :id])))
    (is (= 0x20    (get-in cfg [:socket-two :sid1 :addr])))
    (is (= :mos6581 (get-in cfg [:socket-two :sid1 :type])))
    (is (= 2       (get-in cfg [:socket-two :sid2 :id])))
    (is (= 0x40    (get-in cfg [:socket-two :sid2 :addr])))))

(deftest parse-led
  (let [buf  (make-buf 30 1  31 1)
        cfg  (driver/parse-config-bytes buf)
        buf2 (make-buf 30 0  31 0)
        cfg2 (driver/parse-config-bytes buf2)]
    (is (true?  (get-in cfg  [:led :enabled])))
    (is (true?  (get-in cfg  [:led :idle-breathe])))
    (is (false? (get-in cfg2 [:led :enabled])))
    (is (false? (get-in cfg2 [:led :idle-breathe])))))

(deftest parse-rgbled
  (let [buf (make-buf 40 1  41 0  42 200  43 3)
        cfg (driver/parse-config-bytes buf)]
    (is (true?  (get-in cfg [:rgbled :enabled])))
    (is (false? (get-in cfg [:rgbled :idle-breathe])))
    (is (= 200  (get-in cfg [:rgbled :brightness])))
    (is (= 3    (get-in cfg [:rgbled :sid-to-use])))))

(deftest parse-rgbled-sid-to-use-zero
  (testing "sid-to-use=0 in buf → -1 (off)"
    (let [cfg (driver/parse-config-bytes (make-buf 40 1  43 0))]
      (is (= -1 (get-in cfg [:rgbled :sid-to-use]))))))

(deftest parse-features
  (let [buf (make-buf 51 1  52 1  53 1  54 1  55 1  56 2)
        cfg (driver/parse-config-bytes buf)]
    (is (true? (get-in cfg [:cdc    :enabled])))
    (is (true? (get-in cfg [:webusb :enabled])))
    (is (true? (get-in cfg [:asid   :enabled])))
    (is (true? (get-in cfg [:midi   :enabled])))
    (is (true? (get-in cfg [:fmopl  :enabled])))
    (is (= 2   (get-in cfg [:fmopl  :sidno])))))

; ── config->commands tests ────────────────────────────────────────────────────

(deftest commands-count
  (testing "always emits 23 SET_CONFIG commands"
    (is (= 23 (count (driver/config->commands model/initial-config))))))

(deftest commands-clock-pal
  (let [cfg   (assoc model/initial-config :clock-rate :pal :lock-clockrate false)
        cmds  (driver/config->commands cfg)
        [s i v] (first cmds)]
    (is (= 0x0 s))
    (is (= 1   i))   ; PAL id = 1
    (is (= 0   v)))) ; lock = false

(deftest commands-clock-ntsc-locked
  (let [cfg   (assoc model/initial-config :clock-rate :ntsc :lock-clockrate true)
        [s i v] (first (driver/config->commands cfg))]
    (is (= 0x0 s))
    (is (= 2   i))   ; NTSC id = 2
    (is (= 1   v)))) ; lock = true

(deftest commands-socket-one-enabled
  (let [cfg  (assoc-in model/initial-config [:socket-one :enabled] true)
        cmds (driver/config->commands cfg)
        s1-enabled (second cmds)] ; index 1 = [0x1 0x0 enabled]
    (is (= [0x1 0x0 1] s1-enabled))))

(deftest commands-socket-one-disabled
  (let [cfg  (assoc-in model/initial-config [:socket-one :enabled] false)
        cmds (driver/config->commands cfg)
        s1-enabled (second cmds)]
    (is (= [0x1 0x0 0] s1-enabled))))

(deftest commands-socket-one-chiptype
  (let [cfg  (assoc-in model/initial-config [:socket-one :chiptype] :armsid)
        cmds (driver/config->commands cfg)
        s1-chip (nth cmds 3)] ; index 3 = [0x1 0x2 chiptype-id]
    (is (= [0x1 0x2 3] s1-chip)))) ; ARMSID id = 3

(deftest commands-socket-one-sid-types
  (let [cfg  (-> model/initial-config
                 (assoc-in [:socket-one :sid1 :type] :mos8580)
                 (assoc-in [:socket-one :sid2 :type] :mos6581))
        cmds (driver/config->commands cfg)]
    (is (= [0x1 0x4 2] (nth cmds 4))) ; sid1.type = MOS8580 id=2
    (is (= [0x1 0x5 3] (nth cmds 5))))) ; sid2.type = MOS6581 id=3

(deftest commands-led
  (let [cfg  (assoc model/initial-config :led {:enabled false :idle-breathe true})
        cmds (driver/config->commands cfg)]
    (is (= [0x3 0x0 0] (nth cmds 11))) ; LED enabled=false
    (is (= [0x3 0x1 1] (nth cmds 12))))) ; LED idle-breathe=true

(deftest commands-rgbled-brightness
  (let [cfg  (assoc model/initial-config :rgbled {:enabled true :idle-breathe false
                                                   :brightness 255 :sid-to-use 2})
        cmds (driver/config->commands cfg)]
    (is (= [0x4 0x0 1]   (nth cmds 13))) ; RGB enabled
    (is (= [0x4 0x1 0]   (nth cmds 14))) ; idle-breathe=false
    (is (= [0x4 0x2 255] (nth cmds 15))) ; brightness
    (is (= [0x4 0x3 2]   (nth cmds 16))))) ; sid-to-use=2

(deftest commands-rgbled-sid-negative
  (testing "sid-to-use=-1 → max(0,-1)=0 in command"
    (let [cfg  (assoc model/initial-config :rgbled {:enabled false :idle-breathe false
                                                     :brightness 0 :sid-to-use -1})
          cmds (driver/config->commands cfg)]
      (is (= [0x4 0x3 0] (nth cmds 16))))))

(deftest commands-fmopl
  (let [cfg  (assoc model/initial-config :fmopl {:enabled true :sidno 2})
        cmds (driver/config->commands cfg)]
    (is (= [0x9 1 0] (nth cmds 17))))) ; FMOpl enabled=1

(deftest commands-advanced
  (let [cfg  (assoc model/initial-config
                    :stereo-en true :lock-audio-sw false
                    :mirrored false :flipped true :mixed false)
        cmds (driver/config->commands cfg)]
    (is (= [0xA 1 0] (nth cmds 18))) ; stereo_en
    (is (= [0xB 0 0] (nth cmds 19))) ; lock_audio_sw
    (is (= [0xC 0 0] (nth cmds 20))) ; mirrored
    (is (= [0xD 1 0] (nth cmds 21))) ; flipped
    (is (= [0xE 0 0] (nth cmds 22))))) ; mixed

(deftest commands-sections-match-c-tool
  (testing "section indices match cfg_usbsid.c write_config"
    (let [cmds (into {} (map (fn [[s i _]] [[s i] true]) (driver/config->commands model/initial-config)))]
      ; From write_config in cfg_usbsid.c
      (is (get cmds [0x0 1]))  ; clock_rate / lock combo - id varies
      (is (get cmds [0x1 0x0])) ; socketOne.enabled
      (is (get cmds [0x1 0x1])) ; socketOne.dualsid
      (is (get cmds [0x1 0x2])) ; socketOne.chiptype
      (is (get cmds [0x1 0x4])) ; socketOne.sid1.type
      (is (get cmds [0x1 0x5])) ; socketOne.sid2.type
      (is (get cmds [0x2 0x0])) ; socketTwo.enabled
      (is (get cmds [0x3 0x0])) ; LED.enabled
      (is (get cmds [0x4 0x0])) ; RGBLED.enabled
      (is (get cmds [0x9 0]))   ; FMOpl
      (is (get cmds [0xC 0]))   ; mirrored
      (is (get cmds [0xD 0]))   ; flipped
      (is (get cmds [0xE 0])))))  ; mixed
