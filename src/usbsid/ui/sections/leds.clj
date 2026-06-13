(ns usbsid.ui.sections.leds
  "I shine bright like a diamond!"
  (:require
   [usbsid.config-model :as model]
   [usbsid.events :as events]
   [usbsid.ui.sections.common :as common]
   [usbsid.ui.widgets :as w]))


;;; Something to do with shiny stuff

(defn leds-section
  "Bright light, bright light!"
  [{:keys [config hover-popup]}]
  (let [led (:led config)
        rgb (:rgbled config)]
    {:fx/type     :v-box
     :style-class "c64-vbox"
     :spacing     8
     :padding     {:top    8
                   :right  8
                   :bottom 8
                   :left   8}
     :children
     [(w/c64-header "LED CONFIGURATION")

      {:fx/type :h-box
       :spacing 12
       :children
       [{:fx/type     :v-box
         :style-class "c64-section"
         :spacing     6
         :children
         [(w/c64-row "LED Enabled"
                     (common/toggles
                      :onoff
                      {:fx/type     :toggle-button
                       :text        (if (:enabled led) "ON " "OFF")
                       :selected    (:enabled led)
                       :on-action   {:event/type :config-changed
                                     :path       [:led :enabled]
                                     :value      (not (:enabled led))}
                       :style-class "c64-toggle"}
                      {:hover-popup hover-popup
                       :hover-text  "Disable or enable the onboard LED"
                       :popup-key   [:led :enabled]}))

          (w/c64-row "LED Idle Breathe"
                     (common/toggles
                      :onoff
                      {:fx/type     :toggle-button
                       :text        (if (:idle-breathe led) "ON " "OFF")
                       :selected    (:idle-breathe led)
                       :disable     (not (:enabled led))
                       :on-action   {:event/type :config-changed
                                     :path       [:led :idle-breathe]
                                     :value      (not (:idle-breathe led))}
                       :style-class "c64-toggle"}
                      {:hover-popup hover-popup
                       :hover-text  "Disable or enable the breathing effect for the onboard the LED"
                       :popup-key   [:led :breathing]})
                     )]}]}

      (w/c64-separator)
      (w/c64-header "RGB LED CONFIGURATION")

      {:fx/type :h-box
       :spacing 12
       :children
       [{:fx/type     :v-box
         :style-class "c64-section"
         :spacing     6
         :children
         [(w/c64-row "RGB LED Enabled"
                     (common/toggles
                      :onoff
                      {:fx/type     :toggle-button
                       :text        (if (:enabled rgb) "ON " "OFF")
                       :selected    (:enabled rgb)
                       :on-action   {:event/type :config-changed
                                     :path       [:rgbled :enabled]
                                     :value      (not (:enabled rgb))}
                       :style-class "c64-toggle"}
                      {:hover-popup hover-popup
                       :hover-text  "Disable or enable the onboard RGB LED (Only works if it's actually present)"
                       :popup-key   [:rgbled :enabled]}))

          (w/c64-row "RGB Idle Breathe"
                     (common/toggles
                      :onoff
                      {:fx/type     :toggle-button
                       :text        (if (:idle-breathe rgb) "ON " "OFF")
                       :selected    (:idle-breathe rgb)
                       :disable     (not (:enabled rgb))
                       :on-action   {:event/type :config-changed
                                     :path       [:rgbled :idle-breathe]
                                     :value      (not (:idle-breathe rgb))}
                       :style-class "c64-toggle"}
                      {:hover-popup hover-popup
                       :hover-text  "Disable or enable the the breathing effect for the onboard RGB LED"
                       :popup-key   [:rgbled :breathing]}))

          (w/c64-row "RGB SID to use"
                     {:fx/type          :combo-box
                      :style-class      ["combo-box" "c64-combo-box"]
                      :items            (mapv :label model/sid-to-use-options)
                      :value            (:label
                                         (first
                                          (filter
                                           (comp #{(:sid-to-use rgb)} :key)
                                           model/sid-to-use-options)))
                      :disable          (not (:enabled rgb))
                      :on-value-changed (fn [v]
                                          (when-let [s (first
                                                        (filter
                                                         (comp #{v} :label)
                                                         model/sid-to-use-options))]
                                            (events/config-changed!
                                             [:rgbled :sid-to-use] (:key s))))})

          (w/c64-labeled-slider (format "%-20s" "Brightness") 0 255 (:brightness rgb)
                                (fn [v]
                                  (events/config-changed!
                                   [:rgbled :brightness] (int v)))
                                {:disabled (not (:enabled rgb))})]}]}]}))
