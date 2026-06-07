(ns usbsid.ui.sections.common
  "Don't want to have too much boilerplate"
  (:require
   [usbsid.ui.widgets :as w]))


;;; Where do we want to go today?

(def github-url  "https://github.com/LouDnl/USBSID-Pico")
(def release-url "https://github.com/LouDnl/USBSID-Pico/releases")


;;; Gotta put it somewhere!?

(def welcome-text-block
  {:fx/type :v-box ; Using a v-box leaves less space between text lines, which is nicer! ^_^
   :children
   [(w/c64-header "WELCOME!")
    {:fx/type     :label
     :text        "Thank you for using USBSID-Pico! After clicking 'CONNECT' the tool will automatically load your boards configuration."
     :style-class ["c64-label" "c64-text-wrap"]}]})

(def info-text-block
  {:fx/type :v-box
   :children
   [{:fx/type     :label
     :text        "(i) Most config items (tabs on the left) are enabled by default"
     :style-class ["c64-label" "c64-warning-text" " c64-text-wrap"]}
    {:fx/type     :label
     :text        "This way you can create your preferred config and save it as ini and reload and apply it at a later time."
     :style-class ["c64-label" "c64-text-wrap"]}]})

(def warning-text-block
  {:fx/type :v-box
   :children
   [{:fx/type     :label
     :text        "(!) Any config setting change must be applied before taking effect."
     :style-class ["c64-label" "c64-error-text" "c64-text-wrap"]}
    {:fx/type     :label
     :text        "You can change multiple config items before applying."
     :style-class ["c64-label" "c64-text-wrap"]}
    {:fx/type     :label
     :text        "Click APPLY CFG, SAVE (NO REBOOT) or SAVE + REBOOT."
     :style-class ["c64-label" "c64-text-wrap"]}]})

(def about-text-block
  {:fx/type :v-box
   :children
   [(w/c64-header "ABOUT USBSID-PICO")

    {:fx/type     :label
     :text        "USBSID-Pico is a Raspberry Pi Pico / Pico 2 based board for interfacing"
     :style-class ["c64-label" "c64-text`-wrap"]}
    {:fx/type     :label
     :text        "one or two MOS SID chips and/or hardware SID emulators over USB with your computer."
     :style-class ["c64-label" "c64-text-wrap"]}]})

(defn deviceinfo-text-block
  [fw pcb]
  {:fx/type :v-box
   :children
   [(w/c64-header "DEVICE INFO")
    {:fx/type     :h-box
     :style-class "c64-hbox"
     :children
     [{:fx/type :label :text "Firmware version:" :style-class "c64-label-dim"}
      {:fx/type :label :text (or (when fw (str "v" fw)) "---") :style-class "c64-label-bright"}]}
    {:fx/type     :h-box
     :style-class "c64-hbox"
     :children
     [{:fx/type :label :text "PCB version:" :style-class "c64-label-dim"}
      {:fx/type :label :text (or (when pcb (str "v" pcb)) "---") :style-class "c64-label-bright"}]}]})

(def credits-text-block
  {:fx/type :v-box
   :children
   [(w/c64-header "CREDITS")
    {:fx/type :label :text "PCB, Firmware & config tool by LouD" :style-class "c64-label"}
    {:fx/type :label :text "Software license: GPLv2" :style-class "c64-label-dim"}
    {:fx/type :label :text "Firmware license: GNUv2" :style-class "c64-label-dim"}
    {:fx/type :label :text "Hardware license: Creative Commons" :style-class "c64-label-dim"}]})

(def revisions-text-block
  {:fx/type :v-box
   :children
   [(w/c64-header "PCB REVISIONS")
    {:fx/type     :label
     :text        "v1.0 / v1.2 : Standard - no audio switch."
     :style-class "c64-label"}
    {:fx/type     :label
     :text        "v1.3+       : Audio switch (mono/stereo)."
     :style-class "c64-label"}
    {:fx/type     :label
     :text        "v1.5+       : Automatic voltage configuration"
     :style-class "c64-label" }
    {:fx/type     :label
     :text        "              with socket change detection + confirmation."
     :style-class "c64-label" }]})

(def links-text-block
  {:fx/type :v-box
   :children
   [(w/c64-header "LINKS")
    {:fx/type     :h-box
     :style-class "c64-hbox"
     :spacing     8
     :children
     [{:fx/type     :button
       :text        "GITHUB"
       :on-action   {:event/type :open-url :url github-url}
       :style-class "c64-button"}
      {:fx/type     :button
       :text        "FIRMWARE RELEASES"
       :on-action   {:event/type :open-url :url release-url}
       :style-class "c64-button"}]}]})
