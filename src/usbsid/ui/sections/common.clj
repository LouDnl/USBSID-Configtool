(ns usbsid.ui.sections.common
  "Don't want to have too much boilerplate"
  (:require
   [usbsid.ui.mouse-events :as me]
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
     :style-class ["c64-label" "c64-warning-text" "c64-text-wrap"]}
    {:fx/type     :label
     :text        "This way you can create your preferred config and save it as ini and reload and apply it at a later time."
     :style-class ["c64-label" "c64-text-wrap"]}]})

(def warning-text-block
  {:fx/type :v-box
   :children
   [{:fx/type     :label
     :text        "(!v0.7.0+) Any change to a configuration setting will be automatically written to USBSID memory."
     :style-class ["c64-label" "c64-error-text" "c64-text-wrap"]}
    {:fx/type     :label
     :text        "When finished with changing the configuration, click `SAVE` to apply it and write to USBSID flash"
     :style-class ["c64-label" "c64-text-wrap"]}
    {:fx/type     :label
     :text        "Hover the buttons below with your mouse to see the exact function of them"
     :style-class ["c64-label" "c64-text-wrap"]}]})

(def warning-text-block-legacy
  {:fx/type :v-box
   :children
   [{:fx/type     :label
     :text        "(!v0.6.4 and lower) Any config setting change must be written and applied before taking effect."
     :style-class ["c64-label" "c64-error-text" "c64-text-wrap"]}
    {:fx/type     :label
     :text        "You can change multiple config items before writing, applying and saving. This way you can test before actually saving."
     :style-class ["c64-label" "c64-text-wrap"]}
    {:fx/type     :label
     :text        "Writing, applying and saving are separate processes in the firmware."
     :style-class ["c64-label" "c64-text-wrap"]}
    {:fx/type     :label
     :text        "Hover the buttons below with your mouse to see the exact function of them"
     :style-class ["c64-label" "c64-text-wrap"]}]})

(def about-text-block
  {:fx/type :v-box
   :children
   [(w/c64-header "ABOUT USBSID-PICO")

    {:fx/type     :label
     :text        "USBSID-Pico is a Raspberry Pi Pico / Pico 2 based board for interfacing"
     :style-class ["c64-label" "c64-text-wrap"]}
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
     [{:fx/type     :label
       :text        "Firmware version:"
       :style-class "c64-label-dim"}
      {:fx/type     :label
       :text        (or (when fw (str "v" fw)) "---")
       :style-class "c64-label-bright"}]}
    {:fx/type     :h-box
     :style-class "c64-hbox"
     :children
     [{:fx/type     :label
       :text        "PCB version:"
       :style-class "c64-label-dim"}
      {:fx/type     :label
       :text        (or (when pcb (str "v" pcb)) "---")
       :style-class "c64-label-bright"}]}]})

(def credits-text-block
  {:fx/type :v-box
   :children
   [(w/c64-header "CREDITS")
    {:fx/type     :label
     :text        "PCB, Firmware & config tool by LouD"
     :style-class "c64-label"}
    {:fx/type     :label
     :text        "Software license: GPLv2"
     :style-class "c64-label-dim"}
    {:fx/type     :label
     :text        "Firmware license: GNUv2"
     :style-class "c64-label-dim"}
    {:fx/type     :label
     :text        "Hardware license: Creative Commons"
     :style-class "c64-label-dim"}]})

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
     :style-class "c64-label"}
    {:fx/type     :label
     :text        "              with socket change detection + confirmation."
     :style-class "c64-label"}]})

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
       :on-action   {:event/type :open-url
                     :url        github-url}
       :style-class "c64-button"}
      {:fx/type     :button
       :text        "FIRMWARE RELEASES"
       :on-action   {:event/type :open-url
                     :url        release-url}
       :style-class "c64-button"}]}]})

(defmacro popup
  [hover-popup hover-key hover-text hover-item]
  `(me/popup
    {:hover-popup ~hover-popup
     :popup-key   ~hover-key
     :popup-text  ~hover-text
     :button      ~hover-item}))

(defn toggles
  "`toggle` selects the popup variant (currently only `:onoff`).
   `:popup-key` must be unique per call site so popups don't fire on every
   sibling at once - pass something like `[socket-key :enabled]`. Defaults to
   `toggle` for back-compat, which is NOT unique when reused across sockets."
  [toggle item & {:keys [hover-popup hover-text popup-key]}]
  (toggle
   {:onoff (popup
            hover-popup
            (or popup-key toggle)
            (or hover-text "ON/OFF:\nClick to turn this feature ON or OFF")
            item)}))

(defn buttons
  [button & {:keys [connected? hover-popup]}]
  (button
   {:read         (popup
                   hover-popup
                   button
                   "READ CONFIG:\nread the active configuration from USBSID ram into the configtool."
                   {:fx/type     :button
                    :text        "READ CFG"
                    :on-action   {:event/type :read-config}
                    :disable     (not connected?)
                    :wrap-text   true
                    :style-class "c64-button"})
    :write        (popup
                   hover-popup
                   button
                   "WRITE CONFIG:\nwrite the configtool configuration to USBSID ram. An apply or save is needed after writing to activate the configuration."
                   {:fx/type     :button
                    :text        "WRITE CFG"
                    :on-action   {:event/type :write-config}
                    :disable     (not connected?)
                    :wrap-text   true
                    :style-class "c64-button"})
    :apply        (popup
                   hover-popup
                   button
                   "APPLY CONFIG:\napply the configuration present in USBSID ram for direct use."
                   {:fx/type     :button
                    :text        "APPLY CFG"
                    :on-action   {:event/type :apply-config}
                    :disable     (not connected?)
                    :wrap-text   true
                    :style-class "c64-button"})
    :save         (popup
                   hover-popup
                   button
                   "SAVE CONFIG:\nsave the active configuration to USBSID flash memory, then load and apply the saved configuration into USBSID ram for direct use."
                   {:fx/type     :button
                    :text        "SAVE CFG"
                    :on-action   {:event/type :save-noreset}
                    :disable     (not connected?)
                    :wrap-text   true
                    :style-class "c64-button-primary"})
    :save-reboot  (popup
                   hover-popup
                   button
                   "SAVE CONFIG & REBOOT:\nsave the active configuration to USBSID flash memory and reboot/power cycle after saving. The configuration is then automatically applied."
                   {:fx/type     :button
                    :text        "+REBOOT"
                    :on-action   {:event/type :save-config}
                    :disable     (not connected?)
                    :wrap-text   true
                    :style-class "c64-button-primary"})
    :reload-flash (popup
                   hover-popup
                   button
                   "RELOAD FROM FLASH:\nReload the last saved configuration from USBSID flash memory and apply it for direct use"
                   {:fx/type     :button
                    :text        "RELOAD"
                    :on-action   {:event/type :reload-flash}
                    :disable     (not connected?)
                    :wrap-text   true
                    :style-class "c64-button-danger"})
    :save-ini     (popup
                   hover-popup
                   button
                   "SAVE INI:\nwrite the current configuration to an .ini file on disk so it can be reloaded later."
                   {:text        "SAVE INI"
                    :on-action   {:event/type :export-ini}
                    :style-class "c64-button"})

    :load-ini     (popup
                   hover-popup
                   button
                   "LOAD INI:\nload a previously saved .ini configuration into the tool (does not write to the board)."
                   {:text        "LOAD INI"
                    :on-action   {:event/type :import-ini}
                    :style-class "c64-button"})
    :reset-dflts  (popup
                   hover-popup
                   button
                   "RESET DEFAULTS:\nReset the USBSID-Pico configuration to default settings and reboot/power cycle."
                   {:fx/type     :button
                    :text        "RESET DEFAULTS"
                    :on-action   {:event/type :reset-config}
                    :disable     (not connected?)
                    :style-class "c64-button-danger"})}))
