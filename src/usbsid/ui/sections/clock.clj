(ns usbsid.ui.sections.clock
  "No, not a regular wall clock I am"
  (:require
   [usbsid.config-model :as model]
   [usbsid.events :as events]
   [usbsid.ui.sections.common :as common]
   [usbsid.ui.widgets :as w]))

(defn clock-section
  "I've seen better time"
  [{:keys [config connection hover-popup]}]
  (let [pcbverint (or (model/parse-pcb-version (:pcb-version connection)) 99)]
    {:fx/type     :v-box
     :style-class "c64-vbox"
     :spacing     8
     :padding     {:top    8
                   :right  8
                   :bottom 8
                   :left   8}
     :children
     [(w/c64-header "CLOCK CONFIGURATION")

      {:fx/type  :h-box
       :spacing  12
       :children
       [{:fx/type     :v-box
         :style-class "c64-section"
         :spacing     6
         :children
         [(w/c64-narrow-row "Clock Rate"
                            (w/c64-combo
                             model/clock-rates
                             (:clock-rate config)
                             (fn [v]
                               (when-let [c (first (filter (comp #{v} :label) model/clock-rates))]
                                 (events/config-changed! [:clock-rate] (:key c))))
                             {:disabled      (or
                                              (:lock-clockrate config)
                                              (:external-clock config))
                              :comboboxclass "c64-combo-box-wide"}))

          (w/c64-narrow-row "Lock Clock Rate"
                            (common/toggles
                             :onoff
                             (w/c64-toggle
                              (:lock-clockrate config)
                              {:event/type :config-changed
                               :path       [:lock-clockrate]
                               :value      (not (:lock-clockrate config))}
                              {:disabled (:external-clock config)})
                             {:hover-popup hover-popup
                              :hover-text  "Lock the current clock rate from being changed"
                              :popup-key   [:clock-rate :locked]}))

          (w/c64-narrow-row "External Clock"
                            (common/toggles
                             :onoff
                             (w/c64-toggle
                              (:external-clock config)
                              {:event/type :config-changed
                               :path       [:external-clock]
                               :value      (not (:external-clock config))}
                              {:disabled
                               (or
                                (> pcbverint 10)
                                (:lock-clockrate config))})
                             {:hover-popup hover-popup
                              :hover-text  "Use an external clock source (v1.0 PCB's only!)"
                              :popup-key   [:external-clock]}))]}]}]}))
