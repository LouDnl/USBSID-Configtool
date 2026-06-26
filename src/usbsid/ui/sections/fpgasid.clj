(ns usbsid.ui.sections.fpgasid
  "F51D"
  (:require
   [clojure.string :as string]
   [usbsid.events :as events]
   [usbsid.sid.fpgasid
    :refer [outputmode_p clear-config
            read-config re-read-config]]
   [usbsid.ui.sections.common
    :refer [popup]]
   [usbsid.ui.widgets :as w]))


;;; Internals

(def ^:private active_socket (atom nil)) ; defaults to socket the fpgasid is in (or one if in both)

(def ^:private active_slot (atom nil)) ; defaults to A on tab open


;;; F51D

(defn sid-section
  [s n sidconfig
   & {:keys [hover-popup]}]
  {:fx/type     :v-box
   :style-class "c64-vbox"
   :spacing     12
   :padding     {:top    8
                 :right  8
                 :bottom 8
                 :left   0}
   :children
   (remove nil?
           [{:fx/type     :label
             :text        (format "Slot %s SID %d" (:slot sidconfig) (:sidno sidconfig))
             :style-class "c64-label-dim"}
            (if (= n 1)
              (w/c64-narrow-row "Output mode"
                                (popup
                                 hover-popup
                                 [:fpgasid s n :outputmode]
                                 (get outputmode_p (get sidconfig :outputmode))
                                 {:fx/type     :label
                                  :text        (get sidconfig :outputmode)
                                  :style-class "c64-label-dim"}))
              (w/c64-narrow-row "" {:fx/type     :label
                                    :text        ""
                                    :style-class "c64-label-dim"}))
            (if (= n 2)
              (w/c64-narrow-row "SID 2 addresses"
                                {:fx/type     :label
                                 :text        (string/join ", " (remove empty? (get sidconfig :sid2addr)))
                                 :style-class "c64-label-dim"})
              (w/c64-narrow-row "SID 1 address"
                                {:fx/type     :label
                                 :text        "$d400" ; SID 1 is always $d400
                                 :style-class "c64-label-dim"}))
            (w/c64-narrow-row "ExtIn source"
                              {:fx/type     :label
                               :text        (get sidconfig :extinsource)
                               :style-class "c64-label-dim"})
            (w/c64-narrow-row "Register read"
                              {:fx/type     :label
                               :text        (get sidconfig :readback)
                               :style-class "c64-label-dim"})
            (w/c64-narrow-row "Register delay"
                              {:fx/type     :label
                               :text        (get sidconfig :regdelay)
                               :style-class "c64-label-dim"})
            (w/c64-narrow-row "Mixed waveform"
                              {:fx/type     :label
                               :text        (get sidconfig :mixedwave)
                               :style-class "c64-label-dim"})
            (w/c64-narrow-row "Crunchy DAC"
                              {:fx/type     :label
                               :text        (get sidconfig :crunchydac)
                               :style-class "c64-label-dim"})
            (w/c64-narrow-row "Analog filter"
                              {:fx/type     :label
                               :text        (get sidconfig :filtermode)
                               :style-class "c64-label-dim"})
            (w/c64-narrow-row "DigiFIX value"
                              {:fx/type     :label
                               :text        (str (get sidconfig :digifx))
                               :style-class "c64-label-dim"})])})

(defn index-config
  [s chipconfig
   & {:keys [hover-popup]}]
  (let [index (:index chipconfig)
        idx   (if (= s "A") :idxa :idxb)
        flt   (if (= s "A") :flta :fltb)]
    {:fx/type     :v-box
     :style-class "c64-vbox"
     :spacing     12
     :padding     {:top    8
                   :right  8
                   :bottom 8
                   :left   0}
     :children
     [(w/c64-narrow-row (format "Index config %s" s)
                        {:fx/type     :label
                         :text        (format "%02X" (get chipconfig idx))
                         :style-class "c64-label-dim"})
      (w/c64-narrow-row (str "SID 1 " s)
                        {:fx/type     :label
                         :text        (apply format "%02X%02X%02X"
                                             (if (= s "A")
                                               (get index :1a)
                                               (get index :1b)))
                         :style-class "c64-label-dim"})
      (w/c64-narrow-row (str "SID 2 " s)
                        {:fx/type     :label
                         :text        (apply format "%02X%02X%02X"
                                             (if (= s "A")
                                               (get index :2a)
                                               (get index :2b)))
                         :style-class "c64-label-dim"})

      {:fx/type     :label
       :text        (format "%-10s" (str "Filterbias " s))
       :style-class "c64-label"
       :wrap-text   false
       :min-width   200}
      (w/c64-narrow-row "  SID1/SID2"
                        {:fx/type     :label
                         :text        (format "%02X" (get chipconfig flt))
                         :style-class "c64-label-dim"})]}))

(defn section-chipconfig
  [chipconfig
   & {:keys [hover-popup]}]
  (let [spinsr (get chipconfig :select_pins)
        spinsb (Integer/toBinaryString spinsr)
        spinsl (- 8 (.length spinsb))
        spinsp (string/join (repeat spinsl "0"))
        spins  (str spinsp spinsb)]
    [{:fx/type :h-box
      :spacing 12
      :children
      [{:fx/type     :v-box
        :style-class "c64-vbox"
        :spacing     12
        :padding     {:top    8
                      :right  8
                      :bottom 8
                      :left   0}
        :children
        [(w/c64-narrow-row "Identifier"
                           {:fx/type     :label
                            :text        (str (get chipconfig :id) " (FPGASID)")
                            :style-class "c64-label-dim"})
         (w/c64-narrow-row "CPLD Revision"
                           {:fx/type     :label
                            :text        (format "$%02X" (get chipconfig :cpld))
                            :style-class "c64-label-dim"})
         (w/c64-narrow-row "FPGA Revision"
                           {:fx/type     :label
                            :text        (format "$%02X" (get chipconfig :fpga))
                            :style-class "c64-label-dim"})
         (w/c64-narrow-row "PCA Revision"
                           {:fx/type     :label
                            :text        (format "$%02X" (get chipconfig :pca))
                            :style-class "c64-label-dim"})
         (w/c64-narrow-row "Unique ID"
                           {:fx/type     :label
                            :text        (get chipconfig :unique_id)
                            :style-class "c64-label-dim"})
         (w/c64-narrow-row "Clock frequency"
                           {:fx/type     :label
                            :text        (format "%.3f μs" (get chipconfig :frequency))
                            :style-class "c64-label-dim"})
         (w/c64-narrow-row "Select pins"
                           {:fx/type     :label
                            :text        (format "$%02X 0b%s" spinsr spins)
                            :style-class "c64-label-dim"})]}
       (index-config "A" chipconfig {:hover-popup hover-popup})
       (index-config "B" chipconfig {:hover-popup hover-popup})]}

     (w/c64-separator)

     {:fx/type     :h-box
      :style-class "c64-vbox"
      :spacing     12
      :padding     {:top    8
                    :right  0
                    :bottom 8
                    :left   0}
      :children
      [(sid-section "A" 1 (:sid1 chipconfig) {:hover-popup hover-popup})
       (sid-section "A" 2 (:sid2 chipconfig) {:hover-popup hover-popup})]}

     (w/c64-separator)

     {:fx/type     :h-box
      :style-class "c64-vbox"
      :spacing     12
      :padding     {:top    8
                    :right  0
                    :bottom 8
                    :left   0}
      :children
      [(sid-section "B" 1 (:sid3 chipconfig) {:hover-popup hover-popup})
       (sid-section "B" 2 (:sid4 chipconfig) {:hover-popup hover-popup})]}

     (w/c64-separator)]))

(defn fpgasid-section
  "I hold the one true specials!"
  [{:keys [config connection hover-popup]}]
  (let [connected?   (= :connected (:status connection))
        chipone?     (= (get connection :chipone) :fpgasid)
        chiptwo?     (= (get connection :chiptwo) :fpgasid)
        items        (cond-> []
                       chipone? (conj "One")
                       chiptwo? (conj "Two"))
        socket       (do
                       (when (nil? @active_socket)
                         (reset! active_slot "A") ; also reset the active slot to default A
                         (reset! active_socket (if chipone? 1 (if chiptwo? 2 1)))) ; Falls back to 1 (for development)
                       (condp = @active_socket
                         1 :socket-one
                         2 :socket-two))
        socketcfg    (get config socket)
        chipaddr     (get-in socketcfg [:sid1 :addr])
        chipconfig   (read-config connected? chipaddr)] ; ISSUE: Need better fallback?
    {:fx/type     :v-box
     :style-class "c64-vbox"
     :spacing     12
     :padding     {:top    8
                   :right  8
                   :bottom 8
                   :left   8}
     :children
     (into
      [(w/c64-header "FPGASID CONFIGURATION")
       {:fx/type     :label
        :text        "Configuration is automatically read the first time you open this tab or select another socket (when you have 2 FPGASID)"
        :style-class ["c64-label" "c64-text-wrap"]}
       (w/c64-separator)
       (w/c64-header (format "FPGASID in Socket %s at address $%02x"
                             (condp = @active_socket 1 "One" 2 "Two" "Error")
                             (get chipconfig :read_addr)))
       (w/c64-narrow-row "Select socket"
                         {:fx/type          :combo-box
                          :style-class      ["combo-box" "c64-combo-box"]
                          :items            items
                          :value            (condp = @active_socket 1 "One" 2 "Two" "Error")
                          :disable          (or
                                             (nil? @active_socket)
                                             (<= (count items) 1))
                          :on-value-changed (fn [v]
                                              (reset! active_socket (condp = v "One" 1 "Two" 2 nil))
                                              (clear-config)
                                              (events/handle {:event/type :refresh}))})
       (w/c64-separator)
       {:fx/type :h-box
        :spacing 12
        :children
        [(popup
          hover-popup
          [:fpgasid :read-configuration]
          "(Re)Read configuration from FPGASID"
          (w/c64-button "Read Config" (fn [_]
                                        (re-read-config connected? chipaddr)
                                        (events/handle {:event/type :refresh}))))
         (popup
          hover-popup
          [:fpgasid :write-configuration]
          "Write configuration to FPGASID selected slot, does not save to flash!"
          (w/c64-button "Write Config"
                        (fn [_]
                          ; TODO: Finish
                          ; Nothing much here yet :) No impl on ubsid yet
                          #_(events/handle {:event/type :refresh}))
                        {:style-class "c64-button-primary"
                         :disabled true})) ; TODO: Remove disable when there's some actual action here ;)
         (popup
          hover-popup
          [:fpgasid :write-slot-select]
          "Select the slot to write the configuration to"
          (w/c64-smol-row "Slot"
                          {:fx/type          :combo-box
                           :style-class      ["combo-box" "c64-combo-box"]
                           :style            "-fx-min-width: 10px;-fx-max-width: 50px" ; width override, save creating a whole css class ;)
                           :items            ["A" "B"]
                           :value            @active_slot
                           :disable          (nil? @active_slot)
                           :on-value-changed (fn [v]
                                               (reset! active_slot v)
                                               (events/handle {:event/type :refresh}))}))]}]
      (section-chipconfig chipconfig {:hover-popup hover-popup}))}))
