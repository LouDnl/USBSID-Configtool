;; USBSID-Pico is a RPi Pico (RP2040/RP2350) based board for interfacing one
;; or two MOS SID chips and/or hardware SID emulators over (WEB)USB with your
;; computer, phone or ASID supporting player.
;;
;; mouse_events.clj (Configtool)
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

(ns usbsid.ui.mouse-events
  "Wiggle, wiggle, wiggle!"
  (:require
   [cljfx.api :as fx]
   [cljfx.lifecycle :as lifecycle]
   [cljfx.mutator :as mutator])
  (:import
   [javafx.scene Node]
   [javafx.stage Popup]))


;;; In you're face!

(def popup-width 320)

;; Anchors a Popup to a Node by horizontally centering it under the node's
;; top-left corner mapped to screen coords.
(def prop-shown-on
  (fx/make-prop
   (mutator/adder-remover
    (fn [^Popup popup ^Node node]
      (let [bounds   (.getBoundsInLocal node)
            ;; Anchor popup's bottom-left 4px above the button's top edge -
            ;; combined with :anchor-location :window-bottom-left the popup
            ;; grows upward and never overlaps the trigger (overlap would
            ;; fire MOUSE_EXITED and cause a cursor-flicker loop).
            node-pos (.localToScreen node
                                     (* 0.5 (.getWidth bounds))
                                     -4.0)]
        (.show popup node
               (- (.getX node-pos) (* 0.5 popup-width))
               (.getY node-pos))))
    (fn [^Popup popup _]
      (.hide popup)))
   lifecycle/dynamic))

(defn popup
  "Wrap a button so it triggers a hover popup. Args:
     :hover-popup  - current popup key from app state (nil = none shown)
     :popup-key    - this button's id; popup shown when (= hover-popup popup-key)
     :popup-text   - text shown in the popup body
     :button       - button prop map without :fx/type (text/style-class/on-action/etc.)"
  [{:keys [hover-popup popup-key popup-text button]}]
  (let [show?   (= hover-popup popup-key)
        ;; Preserve caller's :fx/type (toggle-button has :selected,
        ;; plain :button doesn't). Default to :button when caller omits.
        trigger (-> button
                    (update :fx/type #(or % :button))
                    (assoc  :on-mouse-entered {:event/type :popup-show
                                               :key        popup-key}
                            :on-mouse-exited  {:event/type :popup-hide
                                               :key        popup-key}))]
    {:fx/type fx/ext-let-refs
     :refs    {::trigger trigger}
     :desc    {:fx/type fx/ext-let-refs
               :refs    (cond-> {}
                          show?
                          (assoc ::popup
                                 {:fx/type         :popup
                                  :anchor-location :window-bottom-left
                                  :auto-hide       false
                                  :auto-fix        true
                                  :content         [{:fx/type     :label
                                                     :style-class "c64-popup"
                                                     :min-width   popup-width
                                                     :max-width   popup-width
                                                     :pref-width  popup-width
                                                     :wrap-text   true
                                                     :effect      {:fx/type :drop-shadow}
                                                     :text        popup-text}]
                                  prop-shown-on    {:fx/type fx/ext-get-ref
                                                    :ref     ::trigger}}))
               :desc    {:fx/type fx/ext-get-ref
                         :ref     ::trigger}}}))
