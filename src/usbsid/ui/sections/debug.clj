(ns usbsid.ui.sections.debug
  "What are you doing, stop looking at me!"
  (:require
   [usbsid.ui.widgets :as w]))


;;; The thing

(defn debug-section
  "Do what you want to do, but do it fast!"
  [{:keys [connected?]}]
  {:fx/type     :v-box
   :style-class "c64-vbox"
   :spacing     10
   :padding     {:top    8
                 :right  8
                 :bottom 8
                 :left   8}
   :children
   [(w/c64-header "RESET")

    {:fx/type :h-box
     :spacing 12
     :children
     [{:fx/type     :v-box
       :style-class "c64-section"
       :spacing     6
       :children
       [{:fx/type     :h-box
         :style-class "c64-hbox"
         :spacing     6
         :children
         [{:fx/type     :button
           :text        "RESET MCU"
           :on-action   {:event/type :reset-mcu}
           :disable     (not connected?)
           :style-class "c64-button-danger"}
          {:fx/type     :button
           :text        "BOOTLOADER"
           :on-action   {:event/type :bootloader}
           :disable     (not connected?)
           :style-class "c64-button-danger"}]}
        {:fx/type     :label
         :text        "RESET MCU: USB will disconnect, reconnect required."
         :style-class "c64-label-dim"}
        {:fx/type     :label
         :text        "BOOTLOADER: USB will disconnect and firmware can be copied to USBSID"
         :style-class "c64-label-dim"}]}]}

    (w/c64-separator)
    (w/c64-header "BUS")

    {:fx/type :h-box
     :spacing 12
     :children
     [{:fx/type     :v-box
       :style-class "c64-section"
       :spacing     6
       :children
       [{:fx/type     :label
         :text        "Low-level board debug operations."
         :style-class "c64-label-dim"}
        {:fx/type     :h-box
         :style-class "c64-hbox"
         :spacing     6
         :children
         [{:fx/type     :button
           :text        "RESET SIDS"
           :on-action   {:event/type :reset-sids}
           :disable     (not connected?)
           :style-class "c64-button-danger"}
          {:fx/type     :button
           :text        "RESTART BUS"
           :on-action   {:event/type :restart-bus}
           :disable     (not connected?)
           :style-class "c64-button"}
          {:fx/type     :button
           :text        "RESTART CLK"
           :on-action   {:event/type :restart-clk}
           :disable     (not connected?)
           :style-class "c64-button"}
          {:fx/type     :button
           :text        "SYNC PIOS"
           :on-action   {:event/type :sync-pios}
           :disable     (not connected?)
           :style-class "c64-button"}]}]}]}]})
