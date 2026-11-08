;; USBSID-Pico is a RPi Pico (RP2040/RP2350) based board for interfacing one
;; or two MOS SID chips and/or hardware SID emulators over (WEB)USB with your
;; computer, phone or ASID supporting player.
;;
;; debug.clj (Configtool)
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
           :text        "RESET SID REGISTERS"
           :on-action   {:event/type :reset-sid-registers}
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
