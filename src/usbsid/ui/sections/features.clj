(ns usbsid.ui.sections.features
  "Who are you featuring today?"
  (:require
   [clojure.string :refer [join split]]
   [usbsid.ui.widgets :as w]))


;;; Features, not futures!

(defn parse-pcb-version
  [pcbver]
  (Long/parseLong (join "" (split pcbver #"\."))))

(defn features-section
  "I see, I see what you can't see"
  [{:keys [config connection]}]
  {:fx/type     :v-box
   :style-class "c64-vbox"
   :spacing     8
   :padding     {:top    8
                 :right  8
                 :bottom 8
                 :left   8}
   :children
   [(w/c64-header "USB PROTOCOLS")

    {:fx/type :h-box
     :spacing 12
     :children
     [{:fx/type     :v-box
       :style-class "c64-section"
       :spacing     6
       :children
       [(w/c64-row "CDC (USB Serial)"
                   {:fx/type     :label
                    :text        "Always ON"
                    :style-class "c64-label-dim"})

        (w/c64-row "WebUSB"
                   {:fx/type     :label
                    :text        "Always ON"
                    :style-class "c64-label-dim"})

        (w/c64-row "ASID Protocol"
                   {:fx/type     :toggle-button
                    :text        (if (get-in config [:asid :enabled]) "ON " "OFF")
                    :selected    (get-in config [:asid :enabled])
                    :on-action   {:event/type :config-changed
                                  :path       [:asid :enabled]
                                  :value      (not (get-in config [:asid :enabled]))}
                    :style-class "c64-toggle"})

        (w/c64-row "MIDI"
                   {:fx/type     :toggle-button
                    :text        (if (get-in config [:midi :enabled]) "ON " "OFF")
                    :selected    (get-in config [:midi :enabled])
                    :on-action   {:event/type :config-changed
                                  :path       [:midi :enabled]
                                  :value      (not (get-in config [:midi :enabled]))}
                    :style-class "c64-toggle"})]}]}

    (w/c64-separator)
    (w/c64-header "ADVANCED")

    {:fx/type :h-box
     :spacing 12
     :children
     [{:fx/type     :v-box
       :style-class "c64-section"
       :spacing     6
       :children
       [(w/c64-row "Mirrored"
                   {:fx/type     :toggle-button
                    :text        (if (:mirrored config) "ON " "OFF")
                    :selected    (:mirrored config)
                    :on-action   {:event/type :config-changed
                                  :path       [:mirrored]
                                  :value      (not (:mirrored config))}
                    :style-class "c64-toggle"})

        (w/c64-row "Flipped"
                   {:fx/type     :toggle-button
                    :text        (if (:flipped config) "ON " "OFF")
                    :selected    (:flipped config)
                    :on-action   {:event/type :config-changed
                                  :path       [:flipped]
                                  :value      (not (:flipped config))}
                    :style-class "c64-toggle"})

        (w/c64-row "Mixed (Quad only)"
                   {:fx/type     :toggle-button
                    :text        (if (:mixed config) "ON " "OFF")
                    :selected    (:mixed config)
                    :on-action   {:event/type :config-changed
                                  :path       [:mixed]
                                  :value      (not (:mixed config))}
                    :style-class "c64-toggle"})]}]}

    (w/c64-separator)
    (w/c64-header "PCB v1.3+ Options")

    {:fx/type :h-box
     :spacing 12
     :children
     [{:fx/type     :v-box
       :style-class "c64-section"
       :spacing     6
       :disable    (or (= (:status connection) :disconnected)
                       (and
                        (= (:status connection) :connected)
                        (< (parse-pcb-version (:pcb-version connection)) 13)))
       :children
       [(w/c64-row "Stereo Mode"
                   {:fx/type     :toggle-button
                    :text        (if (:stereo-en config) "ON " "OFF")
                    :selected    (:stereo-en config)
                    :disable     (:lock-audio-sw config)
                    :on-action   {:event/type :config-changed
                                  :path       [:stereo-en]
                                  :value      (not (:stereo-en config))}
                    :style-class "c64-toggle"})
        (w/c64-row "Lock Audio Switch"
                   {:fx/type     :toggle-button
                    :text        (if (:lock-audio-sw config) "ON " "OFF")
                    :selected    (:lock-audio-sw config)
                    :on-action   {:event/type :config-changed
                                  :path       [:lock-audio-sw]
                                  :value      (not (:lock-audio-sw config))}
                    :style-class "c64-toggle"})]}]}



    (w/c64-separator)
    (w/c64-header "PCB v1.5+ Options")

    {:fx/type :h-box
     :spacing 12
     :children
     [{:fx/type     :v-box
       :style-class "c64-section"
       :spacing     6
       :disable    (or (= (:status connection) :disconnected)
                       (and
                        (= (:status connection) :connected)
                        (< (parse-pcb-version (:pcb-version connection)) 15)))
       :children
       [(w/c64-row "Disable Socket Change Detect"
                   {:fx/type     :toggle-button
                    :text        (if (:disable-changedetect config) "ON " "OFF")
                    :selected    (:disable-changedetect config)
                    :on-action   {:event/type :config-changed
                                  :path       [:disable-changedetect]
                                  :value      (not (:disable-changedetect config))}
                    :style-class "c64-toggle"})]}]}]})
