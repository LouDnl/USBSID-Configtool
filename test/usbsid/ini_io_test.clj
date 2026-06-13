(ns usbsid.ini-io-test
  (:require
   [clojure.test        :refer [deftest is testing are]]
   [clojure.string      :as string]
   [usbsid.ini-io       :as ini-io]
   [usbsid.config-model :as model]))

; export helpers

(deftest export-sections-present
  (let [out (ini-io/config->ini model/initial-config "v0.6.0")]
    (are [s] (string/includes? out s)
      "[General]"
      "[socketOne]"
      "[socketTwo]"
      "[Features]"
      "[LED]"
      "[RGBLED]"
      "[FMOPL]"
      "[Audioswitch]")))

(deftest export-version
  (let [out (ini-io/config->ini model/initial-config "v0.6.0-BETA")]
    (is (string/includes? out "version = v0.6.0-BETA"))))

(deftest export-version-nil
  (let [out (ini-io/config->ini model/initial-config nil)]
    (is (string/includes? out "version = v0.0.0-DEFAULT.19000101"))))

(deftest export-clock-pal
  (let [cfg (assoc model/initial-config :clock-rate :pal)
        out (ini-io/config->ini cfg nil)]
    (is (string/includes? out "clock_rate = 985248"))))

(deftest export-clock-ntsc
  (let [cfg (assoc model/initial-config :clock-rate :ntsc)
        out (ini-io/config->ini cfg nil)]
    (is (string/includes? out "clock_rate = 1022727"))))

(deftest export-lock-clockrate
  (testing "true"
    (let [out (ini-io/config->ini (assoc model/initial-config :lock-clockrate true) nil)]
      (is (string/includes? out "lock_clockrate = True"))))
  (testing "false"
    (let [out (ini-io/config->ini (assoc model/initial-config :lock-clockrate false) nil)]
      (is (string/includes? out "lock_clockrate = False")))))

(deftest export-socket-chiptype
  (let [cfg (assoc-in model/initial-config [:socket-one :chiptype] :skpico)
        out (ini-io/config->ini cfg nil)]
    (is (string/includes? out "chiptype = SKPico"))))

(deftest export-socket-chiptype-real
  (let [cfg (assoc-in model/initial-config [:socket-one :chiptype] :real)
        out (ini-io/config->ini cfg nil)]
    (is (string/includes? out "chiptype = MOS"))))

(deftest export-sid-types
  (let [cfg (-> model/initial-config
                (assoc-in [:socket-one :sid1 :type] :mos8580)
                (assoc-in [:socket-one :sid2 :type] :mos6581))
        out (ini-io/config->ini cfg nil)]
    (is (string/includes? out "sid1type = 8580"))
    (is (string/includes? out "sid2type = 6581"))))

(deftest export-dualsid
  (testing "enabled"
    (let [out (ini-io/config->ini (assoc-in model/initial-config [:socket-one :dualsid] true) nil)]
      (is (string/includes? out "dualsid = Enabled"))))
  (testing "disabled"
    (let [out (ini-io/config->ini (assoc-in model/initial-config [:socket-one :dualsid] false) nil)]
      (is (string/includes? out "dualsid = Disabled")))))

(deftest export-features
  (let [cfg (assoc model/initial-config :mirrored true :flipped false :mixed true)
        out (ini-io/config->ini cfg nil)]
    (is (string/includes? out "mirrored = True"))
    (is (string/includes? out "flipped  = False"))
    (is (string/includes? out "mixed    = True"))))

(deftest export-led
  (let [cfg (assoc model/initial-config :led {:enabled true :idle-breathe false})
        out (ini-io/config->ini cfg nil)]
    (is (string/includes? out "[LED]"))
    (is (string/includes? out "idle_breathe = False"))))

(deftest export-rgbled
  (let [cfg (assoc model/initial-config :rgbled {:enabled true :idle-breathe false
                                                  :brightness 200 :sid-to-use 3})
        out (ini-io/config->ini cfg nil)]
    (is (string/includes? out "brightness = 200"))
    (is (string/includes? out "sid_to_use = 3"))))

(deftest export-audioswitch
  (let [cfg (assoc model/initial-config :stereo-en true :lock-audio-sw false)
        out (ini-io/config->ini cfg nil)]
    (is (string/includes? out "set_to = Stereo"))
    (is (string/includes? out "lock_audio_switch = False"))))

; import helpers

(deftest import-clock-pal
  (let [ini "[General]\nclock_rate = 985248\n"
        cfg (ini-io/ini->config ini model/initial-config)]
    (is (= :pal (:clock-rate cfg)))))

(deftest import-clock-ntsc
  (let [ini "[General]\nclock_rate = 1022727\n"
        cfg (ini-io/ini->config ini model/initial-config)]
    (is (= :ntsc (:clock-rate cfg)))))

(deftest import-lock-clockrate
  (let [ini "[General]\nlock_clockrate = True\n"
        cfg (ini-io/ini->config ini model/initial-config)]
    (is (true? (:lock-clockrate cfg)))))

(deftest import-socket-chiptype
  (let [ini "[socketOne]\nchiptype = ARMSID\n"
        cfg (ini-io/ini->config ini model/initial-config)]
    (is (= :armsid (get-in cfg [:socket-one :chiptype])))))

(deftest import-socket-chiptype-compat-real
  (testing "old format 'Real' → :real"
    (let [ini "[socketOne]\nchiptype = Real\n"
          cfg (ini-io/ini->config ini model/initial-config)]
      (is (= :real (get-in cfg [:socket-one :chiptype]))))))

(deftest import-socket-chiptype-compat-clone
  (testing "old format 'Clone' → :unknown"
    (let [ini "[socketOne]\nchiptype = Clone\n"
          cfg (ini-io/ini->config ini model/initial-config)]
      (is (= :unknown (get-in cfg [:socket-one :chiptype]))))))

(deftest import-sidtype-new-format
  (let [ini "[socketOne]\nsid1type = 8580\nsid2type = 6581\n"
        cfg (ini-io/ini->config ini model/initial-config)]
    (is (= :mos8580 (get-in cfg [:socket-one :sid1 :type])))
    (is (= :mos6581 (get-in cfg [:socket-one :sid2 :type])))))

(deftest import-sidtype-old-format
  (testing "MOS8580/MOS6581 compat"
    (let [ini "[socketOne]\nsid1type = MOS8580\nsid2type = MOS6581\n"
          cfg (ini-io/ini->config ini model/initial-config)]
      (is (= :mos8580 (get-in cfg [:socket-one :sid1 :type])))
      (is (= :mos6581 (get-in cfg [:socket-one :sid2 :type]))))))

(deftest import-dualsid
  (let [ini "[socketTwo]\ndualsid = Enabled\n"
        cfg (ini-io/ini->config ini model/initial-config)]
    (is (true? (get-in cfg [:socket-two :dualsid])))))

(deftest import-features
  (let [ini "[Features]\nmirrored = True\nflipped = False\nmixed = True\n"
        cfg (ini-io/ini->config ini model/initial-config)]
    (is (true?  (:mirrored cfg)))
    (is (false? (:flipped cfg)))
    (is (true?  (:mixed cfg)))))

(deftest import-act-as-one-compat
  (testing "socketTwo/act_as_one maps to :mirrored"
    (let [ini "[socketTwo]\nact_as_one = True\n"
          cfg (ini-io/ini->config ini model/initial-config)]
      (is (true? (:mirrored cfg))))))

(deftest import-rgbled
  (let [ini "[RGBLED]\nenabled = True\nbrightness = 200\nsid_to_use = 2\n"
        cfg (ini-io/ini->config ini model/initial-config)]
    (is (true? (get-in cfg [:rgbled :enabled])))
    (is (= 200  (get-in cfg [:rgbled :brightness])))
    (is (= 2    (get-in cfg [:rgbled :sid-to-use])))))

(deftest import-rgbled-brightness-clamp
  (testing "0 is valid min"
    (let [ini "[RGBLED]\nbrightness = 0\n"
          cfg (ini-io/ini->config ini model/initial-config)]
      (is (= 0 (get-in cfg [:rgbled :brightness]))))))

(deftest import-audioswitch
  (let [ini "[Audioswitch]\nset_to = Stereo\nlock_audio_switch = True\n"
        cfg (ini-io/ini->config ini model/initial-config)]
    (is (true? (:stereo-en cfg)))
    (is (true? (:lock-audio-sw cfg)))))

(deftest import-audioswitch-mono
  (let [ini "[Audioswitch]\nset_to = Mono\n"
        cfg (ini-io/ini->config ini model/initial-config)]
    (is (false? (:stereo-en cfg)))))

; round-trip

(deftest round-trip-default-config
  (let [ini (ini-io/config->ini model/initial-config "v0.6.0")
        cfg (ini-io/ini->config ini model/initial-config)]
    (is (= (:clock-rate model/initial-config) (:clock-rate cfg)))
    (is (= (:lock-clockrate model/initial-config) (:lock-clockrate cfg)))
    (is (= (get-in model/initial-config [:socket-one :enabled])
           (get-in cfg [:socket-one :enabled])))
    (is (= (get-in model/initial-config [:socket-one :chiptype])
           (get-in cfg [:socket-one :chiptype])))
    (is (= (get-in model/initial-config [:led :enabled])
           (get-in cfg [:led :enabled])))
    (is (= (:stereo-en model/initial-config) (:stereo-en cfg)))))

(deftest round-trip-quad-armsid
  (let [cfg (-> model/initial-config
                (assoc :clock-rate :pal :lock-clockrate true
                       :mirrored false :flipped false :mixed false)
                (assoc-in [:socket-one :chiptype] :armsid)
                (assoc-in [:socket-one :dualsid] true)
                (assoc-in [:socket-one :sid1 :type] :mos6581)
                (assoc-in [:socket-one :sid2 :type] :mos6581)
                (assoc-in [:socket-two :chiptype] :armsid)
                (assoc-in [:socket-two :dualsid] true)
                (assoc-in [:socket-two :sid1 :type] :mos6581)
                (assoc-in [:socket-two :sid2 :type] :mos6581)
                (assoc :stereo-en true))
        ini  (ini-io/config->ini cfg nil)
        out  (ini-io/ini->config ini model/initial-config)]
    (is (= :pal  (:clock-rate out)))
    (is (true?   (:lock-clockrate out)))
    (is (= :armsid (get-in out [:socket-one :chiptype])))
    (is (true?   (get-in out [:socket-one :dualsid])))
    (is (= :mos6581 (get-in out [:socket-one :sid1 :type])))
    (is (= :armsid (get-in out [:socket-two :chiptype])))
    (is (true?   (:stereo-en out)))))

(deftest parse-real-ini-file
  (testing "quad.ini example values"
    (let [ini "[General]\nclock_rate = 985248\nlock_clockrate = True\n[socketOne]\nenabled = True\ndualsid = Enabled\nchiptype = SKPico\nsid1type = MOS6581\nsid2type = MOS6581\n[socketTwo]\nenabled = True\ndualsid = Enabled\nchiptype = SKPico\nsid1type = MOS6581\nsid2type = MOS6581\n[RGBLED]\nenabled = False\nbrightness = 0\nsid_to_use = 1\n"
          cfg (ini-io/ini->config ini model/initial-config)]
      (is (= :pal      (:clock-rate cfg)))
      (is (true?       (:lock-clockrate cfg)))
      (is (= :skpico   (get-in cfg [:socket-one :chiptype])))
      (is (true?       (get-in cfg [:socket-one :dualsid])))
      (is (= :mos6581  (get-in cfg [:socket-one :sid1 :type])))
      (is (false?      (get-in cfg [:rgbled :enabled])))
      (is (= 0         (get-in cfg [:rgbled :brightness]))))))
