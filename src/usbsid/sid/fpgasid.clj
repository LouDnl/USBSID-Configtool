(ns usbsid.sid.fpgasid
  (:require
  ;;  [usbsid.events :as events]
   [usbsid.state :as state]
   [usbsid.sid.chip :as chip]
   [usbsid.driver :as driver]))

(def slots ["A", "B"])
(def extinsource
  ["analog input",
   "disabled",
   "other SID",
   "digifix (8580)"])
(def readback ; on write only registers
  ["bitrotation 6581",
   "always read value",
   "always read $00",
   "bitrotation 8580"])
(def regdelay ["6581", "8580"])
(def mixedwave ["6581", "8580"])
(def crunchydac ["on (6581)", "off (8580)"])
(def filtermode ["6581", "8580"])
(def outputmode ["stereo", "mono mix"])
(def outputmode_p
  {"stereo"   "dual output over SID1 (socket) & SID2 (external) channels",
   "mono mix" "dual output over SID1 channel (mixed)"})
(def sid2addr ["$d400", "$de00", "$d500", "$d420", ""])

(defn- printsidcfg
  [{:keys [slot
           sidno
           outputmode
           sid2addr
           extinsource
           readback
           regdelay
           mixedwave
           crunchydac
           filtermode
           digifx]}]
  (when (:dev-nonce state/*state)
    (println (format "FPGASID configuration of slot %s and SID %d",
                     slot, sidno))
    (when (= sidno 1)
      (println
       (format "  Output mode    = %s", outputmode))
      (println (format "  SID2 addresses = %s", sid2addr)))
    (println
     (format "  ExtIn source   = %s", extinsource))
    (println
     (format "  Register read  = %s", readback))
    (println
     (format "  Register delay = %s", regdelay))
    (println
     (format "  Mixed wavform  = %s", mixedwave))
    (println
     (format "  Crunchy DAC    = %s", crunchydac))
    (println
     (format "  Analog filter  = %s", filtermode))
    (println
     (format "  DigiFIX value  = %d", digifx))))

(defn- printresult
  [{{[sid_one_a_1, sid_one_a_2, sid_one_a_3] :1a
     [sid_one_b_1, sid_one_b_2, sid_one_b_3] :1b
     [sid_two_a_1, sid_two_a_2, sid_two_a_3] :2a
     [sid_two_b_1, sid_two_b_2, sid_two_b_3] :2b} :index
    :keys [idnum cpld fpga pca unique_id
           frequency select_pins
           idxa idxb flta fltb
           sid1 sid2 sid3 sid4]}]
  (when (:dev-nonce state/*state)
    (println "FPGASID Diagnostic result:")
    (println (format "  Identifier:        %04X (FPGASID)", idnum))
    (println (format "  CPLD Revision:     %02X", cpld))
    (println (format "  FPGA Revision:     %02X", fpga))
    (println (format "  PCA Revision:      %02X", pca))
    (println (format "  Unique identifier: %s", unique_id))
    (println (format "  Clock frequency:   %.3fμs", frequency))
    (println (format "  Select pins:       %02X", select_pins))
    (println (format "  Index config A:    %02X", idxa))
    (println (format "    SID 1 A:         %02X%02X%02X",
                     sid_one_a_1, sid_one_a_2, sid_one_a_3))
    (println (format "    SID 2 A:         %02X%02X%02X",
                     sid_one_b_1, sid_one_b_2, sid_one_b_3))
    (println "    Filterbias A")
    (println (format "      SID1/SID2:     %02x", flta))
    (println (format "  Index config B:    %02X", idxb))
    (println (format "    SID 1 B:         %02X%02X%02X",
                     sid_two_a_1, sid_two_a_2, sid_two_a_3))
    (println (format "    SID 2 B:         %02X%02X%02X",
                     sid_two_b_1, sid_two_b_2, sid_two_b_3))
    (println "    Filterbias B")
    (println (format "      SID1/SID2:     %02x", fltb))
    (printsidcfg sid1)
    (printsidcfg sid2)
    (printsidcfg sid3)
    (printsidcfg sid4)))

(def config-buffer (atom nil))

;;; Development test buffer
(def dev-buffer
  (byte-array [-91, 127, -11, 29, 5, 10, 20, 20, 0, 0, 0, 0, 3, 45, -128, -104, 2, 20, -119, -1, 101, 0, 82, 20, 1, -33, 101, 0, 82,
               0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -113, -1]))

; ISSUE: State is not cleared on disconnect, move to -> state? or events?
(def config-read (atom false))
(def fpgasid-config (atom {}))

(defn parse-read-buffer
  "parse usbsid bytearray to fpgasid config"
  [^bytes buf]
  (let [ba           (fn [i] (bit-and i 0xff))
        g            (fn [i] (driver/ba-get buf i)) ; get byte from buffer
        bag          (fn [i] (ba (g i)))
        b            (fn [i] (driver/cfg-byte (g i)))
        cmd          (b 0)
        ver          (b 1)
        end          (b 62)
        term         (b 63)
        expected     [(ba (driver/cfg-byte (:FPGASID chip/read-ids)))
                      (ba (driver/cfg-byte (:VERIFICATION_BYTE chip/data-ids)))
                      (ba (driver/cfg-byte (:END_BYTE chip/data-ids)))
                      (ba (driver/cfg-byte (:TERMINATION_BYTE chip/data-ids)))]
        ;; verification [cmd ver end term]
        verification [(ba cmd) (ba ver) (ba end) (ba term)]
        idHi         (bag 2)
        idLo         (bag 3)
        cpld         (bag 4)
        fpga         (bag 5)
        pca          (bag 6)
        select_pins  (bag 7)
        idxa         (bag 8)
        idxb         (bag 9)
        flta         (bag 10)
        fltb         (bag 11)
        frequency_h  (bag 12)
        frequency_l  (bag 13)
        frequency_r  (bit-or
                      (bit-shift-left frequency_h 8)
                      frequency_l)
        frequency    (-> (float frequency_r)
                         (/ 10)    ; divide by 10 reads
                         (* 12.5)  ; times kHz
                         (/ 1000)) ; convert to uS
        unique_id_1  (bag 14)
        unique_id_2  (bag 15)
        unique_id_3  (bag 16)
        unique_id_4  (bag 17)
        unique_id_5  (bag 18)
        unique_id_6  (bag 19)
        unique_id_7  (bag 20)
        unique_id_8  (bag 21)
        sid_one_a_1  (bag 22)
        sid_one_a_2  (bag 23)
        sid_one_a_3  (bag 24)
        sid_one_b_1  (bag 25)
        sid_one_b_2  (bag 26)
        sid_one_b_3  (bag 27)
        sid_two_a_1  (bag 28)
        sid_two_a_2  (bag 29)
        sid_two_a_3  (bag 30)
        sid_two_b_1  (bag 31)
        sid_two_b_2  (bag 32)
        sid_two_b_3  (bag 33)

        idnum        (fn [] (bit-or (bit-shift-left idHi 8) idLo))
        id           (fn [] (format "%04X" (idnum)))
        unique_id    (fn [] (format "%02X%02X%02X%02X%02X%02X%02X%02X",
                                    unique_id_1, unique_id_2, unique_id_3, unique_id_4
                                    unique_id_5, unique_id_6, unique_id_7, unique_id_8))
        extin        (fn [two] (bit-shift-right (bit-and two 0xc0) 6))
        readb        (fn [two] (bit-shift-right (bit-and two 0x30) 4))
        regd         (fn [two] (bit-shift-right (bit-and two 0x8) 3))
        mixw         (fn [two] (bit-shift-right (bit-and two 0x4) 2))
        crunch       (fn [two] (bit-shift-right (bit-and two 0x2) 1))
        fltr         (fn [two] (bit-and two 0x1))
        outp         (fn [one] (bit-shift-right (bit-and one 0x8) 3))
        addr1        (fn [one] (if (= (bit-shift-right (bit-and one 0x4) 2) 1) 1 4)) ; de00 or not enabled
        addr2        (fn [one] (if (= (bit-shift-right (bit-and one 0x2) 1) 1) 2 4)) ; d500 or not enabled
        addr3        (fn [one] (if (= (bit-and one 0x1) 1) 3 4)) ; d420 or not enabled
        addr         (fn [a1 a2 a3] (if (= (+ a1 a2 a3) 12) 0 4)) ; d400, 2nd sid and stereo disabled

        verified     (fn []
                       (if (= expected verification)
                         {:verified true}
                         {:verified     false
                          :expected     expected
                          :verification verification}))
        cfg_result       (fn []
                           {:id          (id)
                            :idnum       (idnum)
                            :unique_id   (unique_id)
                            :cpld        cpld
                            :fpga        fpga
                            :pca         pca
                            :unique_id_1 unique_id_1
                            :unique_id_2 unique_id_2
                            :unique_id_3 unique_id_3
                            :unique_id_4 unique_id_4
                            :unique_id_5 unique_id_5
                            :unique_id_6 unique_id_6
                            :unique_id_7 unique_id_7
                            :unique_id_8 unique_id_8
                            :frequency   frequency
                            :select_pins select_pins
                            :idxa        idxa
                            :flta        flta
                            :idxb        idxb
                            :fltb        fltb
                            :index       {:1a [sid_one_a_1, sid_one_a_2, sid_one_a_3]
                                          :1b [sid_one_b_1, sid_one_b_2, sid_one_b_3]
                                          :2a [sid_two_a_1, sid_two_a_2, sid_two_a_3]
                                          :2b [sid_two_b_1, sid_two_b_2, sid_two_b_3]}})
        sidcfg       (fn [slot sidno zero one two] ; config array bytes [zero, one, two]
                       (let [slt  (nth slots slot)
                             opm  (nth outputmode (outp one))
                             s2a1 (nth sid2addr (addr (addr1 one) (addr2 one) (addr3 one))),
                             s2a2 (nth sid2addr (addr1 one))
                             s2a3 (nth sid2addr (addr2 one))
                             s2a4 (nth sid2addr (addr3 one))
                             eti  (nth extinsource (extin two))
                             rba  (nth readback (readb two))
                             rdl  (nth regdelay (regd two))
                             mxw  (nth mixedwave (mixw two))
                             cda  (nth crunchydac (crunch two))
                             flm  (nth filtermode (fltr two))]
                         {:verification verification
                          :slot         slt
                          :sidno        sidno
                          :outputmode   opm
                          :sid2addr     [s2a1, s2a2, s2a3, s2a4]
                          :extinsource  eti
                          :readback     rba
                          :regdelay     rdl
                          :mixedwave    mxw
                          :crunchydac   cda
                          :filtermode   flm
                          :digifx       (unchecked-byte zero)}))
        sid1         (sidcfg 0 1 sid_one_a_1 sid_one_a_2 sid_one_a_3)
        sid2         (sidcfg 0 2 sid_one_b_1 sid_one_b_2 sid_one_b_3)
        sid3         (sidcfg 1 1 sid_two_a_1 sid_two_a_2 sid_two_a_3)
        sid4         (sidcfg 1 2 sid_two_b_1 sid_two_b_2 sid_two_b_3)
        result       (fn []
                       (-> (cfg_result)
                           (assoc :sid1 sid1 :sid2 sid2 :sid3 sid3 :sid4 sid4)
                           (merge (verified))))]
    (if (:verified (verified))
      (do (when (:dev-nonce @state/*state)
            (printresult (result)))
          (result))
      (verified))))

(defn config-to-buffer
  "convert fpgasid config to usbsid bytearray"
  [])

(defn read-fpgasid-configuration
  [addr]
  (state/log! (format "Reading FPGASID configuration @ $%02x" addr))
  (let [result (chip/read-clone-chip :FPGASID addr)]
    (reset! config-buffer result)
    (parse-read-buffer @config-buffer)))

(defn clear-config
  []
  (state/log! "Clearing FPGASID configuration")
  (reset! config-buffer nil)
  (reset! config-read false)
  (reset! fpgasid-config {}))

(defn read-config
  [connected? addr]
  (when-not (:verified @fpgasid-config)
    (state/log! "FPGASID configuration is unverified")
    (clear-config))
  (when (false? @config-read)
    (reset! fpgasid-config
            (if connected? ; read dev-buffer only works when in development mode
              (read-fpgasid-configuration addr)
              (parse-read-buffer dev-buffer)))
    (swap! fpgasid-config assoc :read_addr addr)
    (state/log! "FPGASID configuration read")
    (if-not (:verified @fpgasid-config)
      (reset! config-read false)
      (reset! config-read true)))
  (when-not (:verified @fpgasid-config) (clear-config))
  @fpgasid-config)

(defn re-read-config
  [connected? addr]
  (swap! fpgasid-config dissoc :verified) ; remove verification key
  (read-config connected? addr))
