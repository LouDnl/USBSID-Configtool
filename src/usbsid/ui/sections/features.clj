;; USBSID-Pico is a RPi Pico (RP2040/RP2350) based board for interfacing one
;; or two MOS SID chips and/or hardware SID emulators over (WEB)USB with your
;; computer, phone or ASID supporting player.
;;
;; features.clj (Configtool)
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

(ns usbsid.ui.sections.features
  "Who are you featuring today?"
  (:require
   [clojure.core :as c]
   [usbsid.config-model :as model]
   [usbsid.ui.widgets :as w]
   [usbsid.ui.sections.common :as common]))


;;; Features, not futures!

(defn features-section
  "I see, I see what you can't see"
  [{:keys [config connection hover-popup]}]
  (let [fw-line          (or (:fw-line connection) :v0_7)
        flip-mix-disable (not (model/fw-supports? fw-line :flipped))
        confirm-disable  (not (model/fw-supports? fw-line :socket_change_detect))
        fwverint         (model/parse-fw-version (:fw-version connection))]
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
         (cond->
          [(w/c64-row "CDC (USB Serial)"
                      {:fx/type     :label
                       :text        "Always ON"
                       :style-class "c64-label-dim"})

           (w/c64-row "WebUSB"
                      {:fx/type     :label
                       :text        "Always ON"
                       :style-class "c64-label-dim"})]
           (>= fwverint 70)
           (conj
            (w/c64-row "MIDI"
                       (common/toggles
                        :onoff
                        {:fx/type     :toggle-button
                         :text        (if (get-in config [:midi :enabled]) "ON " "OFF")
                         :selected    (get-in config [:midi :enabled])
                         :on-action   {:event/type :config-changed
                                       :path       [:midi :enabled]
                                       :value      (not (get-in config [:midi :enabled]))}
                         :style-class "c64-toggle"}
                        {:hover-popup hover-popup
                         :hover-text  "Disable or enable the Midi protocol (does not affect ASID)"
                         :popup-key   [:midi :enabled]}))

            (w/c64-row "ASID Protocol"
                       (common/toggles
                        :onoff
                        {:fx/type     :toggle-button
                         :text        (if (get-in config [:asid :enabled]) "ON " "OFF")
                         :selected    (get-in config [:asid :enabled])
                         :on-action   {:event/type :config-changed
                                       :path       [:asid :enabled]
                                       :value      (not (get-in config [:asid :enabled]))}
                         :style-class "c64-toggle"}
                        {:hover-popup hover-popup
                         :hover-text  "Disable or enable the ASID protocol (does not affect Midi)`"
                         :popup-key   [:asid :enabled]})))
           (< fwverint 70)
           (conj
            (w/c64-row "MIDI"
                       {:fx/type     :label
                        :text        "Always ON"
                        :style-class "c64-label-dim"})

            (w/c64-row "ASID Protocol"
                       {:fx/type     :label
                        :text        "Always ON"
                        :style-class "c64-label-dim"})))}]}

      (w/c64-separator)
      (w/c64-header "ADVANCED")

      {:fx/type :h-box
       :spacing 12
       :children
       [{:fx/type     :v-box
         :style-class "c64-section"
         :spacing     6
         :children
         (cond->
          [(w/c64-row "Preset auto detect"
                      (common/toggles
                       :onoff
                       {:fx/type     :toggle-button
                        :text        (if (:preset_auto_detect config) "ON " "OFF")
                        :selected    (:preset_auto_detect config)
                        :on-action   {:event/type :config-changed
                                      :path       [:preset_auto_detect]
                                      :value      (not (:preset_auto_detect config))}
                        :style-class "c64-toggle"}
                       {:hover-popup hover-popup
                        :hover-text  "Enable/ Disable the boards silent autodetection during socket preset implementation"
                        :popup-key   [:preset_auto_detect :enabled]}))
           (w/c64-row "Mirrored"
                      (common/toggles
                       :onoff
                       {:fx/type     :toggle-button
                        :text        (if (:mirrored config) "ON " "OFF")
                        :selected    (:mirrored config)
                        :on-action   {:event/type :config-changed
                                      :path       [:mirrored]
                                      :value      (not (:mirrored config))}
                        :style-class "c64-toggle"}
                       {:hover-popup hover-popup
                        :hover-text  "Mirror socket 2 with socket 1, all writes go to both sockets at the same time"
                        :popup-key   [:mirrored :enabled]}))
           (w/c64-row "Flipped"
                      (common/toggles
                       :onoff
                       {:fx/type     :toggle-button
                        :text        (if (:flipped config) "ON " "OFF")
                        :selected    (:flipped config)
                        :disable     (or (< fwverint 70) flip-mix-disable)
                        :on-action   {:event/type :config-changed
                                      :path       [:flipped]
                                      :value      (not (:flipped config))}
                        :style-class "c64-toggle"}
                       {:hover-popup hover-popup
                        :hover-text  "Flip socket 2 with socket 1, making socket 2 become socket 1 and vice versa"
                        :popup-key   [:flipped :enabled]}))

           (w/c64-row "Mixed (Quad only)"
                      (common/toggles
                       :onoff
                       {:fx/type     :toggle-button
                        :text        (if (:mixed config) "ON " "OFF")
                        :selected    (:mixed config)
                        :disable     (or (< fwverint 70) flip-mix-disable)
                        :on-action   {:event/type :config-changed
                                      :path       [:mixed]
                                      :value      (not (:mixed config))}
                        :style-class "c64-toggle"}
                       {:hover-popup hover-popup
                        :hover-text  "Mixup socket 2 with socket 1, sidaddresses are mixed"
                        :popup-key   [:mixed :enabled]}))])}]}

      (w/c64-separator)
      (w/c64-header "PCB v1.3+ Options")

      {:fx/type :h-box
       :spacing 12
       :children
       [{:fx/type     :v-box
         :style-class "c64-section"
         :spacing     6
         :disable     (or (= (:status connection) :disconnected)
                          (and
                           (= (:status connection) :connected)
                           (< (model/parse-pcb-version (:pcb-version connection)) 13)))
         :children
         [(w/c64-row "Stereo Mode"
                     (common/toggles
                      :onoff
                      {:fx/type     :toggle-button
                       :text        (if (:stereo-en config) "ON " "OFF")
                       :selected    (:stereo-en config)
                       :disable     (:lock-audio-sw config)
                       :on-action   {:event/type :config-changed
                                     :path       [:stereo-en]
                                     :value      (not (:stereo-en config))}
                       :style-class "c64-toggle"}
                      {:hover-popup hover-popup
                       :hover-text  "Set the digital audio switch to Stereo or Mono"
                       :popup-key   [:stereo :enabled]}))
          (w/c64-row "Lock Audio Switch"
                     (common/toggles
                      :onoff
                      {:fx/type     :toggle-button
                       :text        (if (:lock-audio-sw config) "ON " "OFF")
                       :selected    (:lock-audio-sw config)
                       :on-action   {:event/type :config-changed
                                     :path       [:lock-audio-sw]
                                     :value      (not (:lock-audio-sw config))}
                       :style-class "c64-toggle"}
                      {:hover-popup hover-popup
                       :hover-text  "Lock the audio switch from being changed"
                       :popup-key   [:stereo :locked]}))]}]}

      (w/c64-separator)
      (w/c64-header "PCB v1.5+ Options")

      {:fx/type :h-box
       :spacing 12
       :children
       [{:fx/type     :v-box
         :style-class "c64-section"
         :spacing     6
         :disable     (or confirm-disable
                          (= (:status connection) :disconnected)
                          (and
                           (= (:status connection) :connected)
                           (< (model/parse-pcb-version (:pcb-version connection)) 15)))
         :children
         [(w/c64-row "Socket Change Detection on boot"
                     (common/toggles
                      :onoff
                      {:fx/type     :toggle-button
                       :text        (if (:socket_change_detect config) "ON " "OFF")
                       :selected    (:socket_change_detect config)
                       :on-action   {:event/type :config-changed
                                     :path       [:socket_change_detect]
                                     :value      (not (:socket_change_detect config))}
                       :style-class "c64-toggle"}
                      {:hover-popup hover-popup
                       :hover-text  "Disable detection of changes in the sockets on boot. Effectively overriding any voltage settings!"
                       :popup-key   [:socket_change_detect :locked]}))]}]}]}))
