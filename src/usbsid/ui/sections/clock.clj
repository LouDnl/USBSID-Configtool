;; USBSID-Pico is a RPi Pico (RP2040/RP2350) based board for interfacing one
;; or two MOS SID chips and/or hardware SID emulators over (WEB)USB with your
;; computer, phone or ASID supporting player.
;;
;; clock.clj (Configtool)
;; This file is part of USBSID-Configtool (https://github.com/LouDnl/USBSID-Configtool)
;; File author: LouD
;;
;; USBSID-Configtool
;; Copyright (C) 2024-2026  LouD
;;
;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.
;;
;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with this program. If not, see <http://www.gnu.org/licenses/>.
;;
;; Additional permission under GNU GPL version 3 section 7
;;
;; If you modify this Program, or any covered work, by linking or combining it
;; with Clojure or any other library licensed under the Eclipse Public License
;; 1.0 or 2.0 (or a modified version of such a library), containing parts
;; covered by the terms of the Eclipse Public License, the licensors of this
;; Program grant you additional permission to convey the resulting work.
;; Corresponding Source for a non-source form of such a combination shall
;; include the source code for the parts of those libraries used as well as
;; that of the covered work.

(ns usbsid.ui.sections.clock
  "No, not a regular wall clock I am"
  (:require
   [usbsid.config-model :as model]
   [usbsid.events :as events]
   [usbsid.ui.sections.common :as common]
   [usbsid.ui.widgets :as w]))


;;; Clockworx

(defn clock-section
  "I've seen better time"
  [{:keys [config connection hover-popup]}]
  (let [_pcb (:pcb-version connection)]
    {:fx/type     :v-box
     :style-class "c64-vbox"
     :spacing     8
     :padding     {:top    8
                   :right  8
                   :bottom 8
                   :left   8}
     :children
     [(w/c64-header "CLOCK CONFIGURATION")

      {:fx/type :h-box
       :spacing 12
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
                             {:fx/type     :label
                              :text        (if (:external-clock config) "ON " "OFF")
                              :style-class "c64-label-dim"}
                             {:hover-popup hover-popup
                              :hover-text  "Display only, the firmware detects an external clock source at startup"
                              :popup-key   [:external-clock]}))

          (w/c64-narrow-row "Clock link"
                            (common/toggles
                             :onoff
                             {:fx/type     :toggle-button
                              :text        (if (:clock-link config) "ON " "OFF")
                              :selected    (boolean (:clock-link config))
                              :disable     (or (not (model/fw-supports? (:fw-line connection) :clock-link))
                                               (not= (:status connection) :connected))
                              :on-action   {:event/type :set-clock-link
                                            :value      (not (:clock-link config))}
                              :style-class "c64-toggle"}
                             {:hover-popup hover-popup
                              :hover-text  "Link the board clock to an external clock input. Changing this saves the configuration and reboots the board"
                              :popup-key   [:clock-link]}))]}]}]}))
