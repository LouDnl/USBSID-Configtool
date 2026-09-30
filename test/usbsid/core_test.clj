;; USBSID-Pico is a RPi Pico (RP2040/RP2350) based board for interfacing one
;; or two MOS SID chips and/or hardware SID emulators over (WEB)USB with your
;; computer, phone or ASID supporting player.
;;
;; core_test.clj (Configtool tests)
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

(ns usbsid.core-test
  (:require
   [clojure.test          :refer [deftest is testing]]
   [usbsid.config-model   :as model]
   [usbsid.state          :as state]))

(deftest namespace-loads
  (testing "config-model namespace loads"
    (is (seq model/clock-rates))
    (is (seq model/chip-types))
    (is (seq model/presets)))
  (testing "state atom initialises"
    (is (map? @state/*state))
    (is (= :disconnected (get-in @state/*state [:connection :status])))))
