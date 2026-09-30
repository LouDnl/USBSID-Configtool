;; USBSID-Pico is a RPi Pico (RP2040/RP2350) based board for interfacing one
;; or two MOS SID chips and/or hardware SID emulators over (WEB)USB with your
;; computer, phone or ASID supporting player.
;;
;; widgets.clj (Configtool)
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

(ns usbsid.ui.widgets
  "Well, widget me this, widget me that!")


;;; Here be widgets!

(defn c64-label
  "I feel so alone"
  ([text] (c64-label text {}))
  ([text {:keys [style-class bright dim]
          :or   {style-class nil
                 bright      false
                 dim         false}}]
   {:fx/type     :label
    :text        text
    :style-class (cond bright "c64-label-bright"
                       dim    "c64-label-dim"
                       style-class style-class
                       :else  "c64-label")}))

(defn c64-header
  "It's up there"
  [text]
  {:pre [(string? text)]}
  {:fx/type     :label
   :text        text
   :style-class "c64-section-header"})

(defn c64-button
  "Stop pushing me"
  ([text on-action] (c64-button text on-action {}))
  ([text on-action {:keys [style-class disabled]
                    :or   {style-class "c64-button"
                           disabled    false}}]
   {:pre [(string? text)]}
   {:fx/type     :button
    :text        text
    :on-action   on-action
    :disable     disabled
    :style-class style-class}))

(defn c64-toggle
  "That tickles!"
  [selected on-action & {:keys [disabled]
                         :or   {disabled false}}]
  {:fx/type     :toggle-button
   :text        (if selected "ON " "OFF")
   :selected    selected
   :on-action   on-action
   :disable     disabled
   :style-class "c64-toggle"})

(defn c64-labeled-toggle
  "Toggle me this, toggle me that"
  [label selected on-action
   & {:keys [disabled]
      :or   {disabled false}}]
  {:fx/type     :h-box
   :style-class "c64-hbox"
   :children
   [{:fx/type     :label
     :text        (format "%-24s" label)
     :style-class "c64-label"
     :min-width   200}
    (c64-toggle selected on-action :disabled disabled)]})

(defn c64-combo
  "Left, right, up, down, x, up, y, right, select, start"
  [items selected-key on-change
   & {:keys [disabled comboboxclass]
      :or   {disabled      false
             comboboxclass "c64-combo-box"}}]
  {:fx/type          :combo-box
   :style-class      ["combo-box" comboboxclass]
   :items            (mapv :label items)
   :value            (when selected-key
                       (-> (filter (comp #{selected-key} :key) items) first :label))
   :on-value-changed on-change
   :disable          disabled})

(defn c64-labeled-combo
  "Use me already"
  [label items selected-key on-change
   & {:keys [disabled]
      :or   {disabled false}}]
  {:fx/type     :h-box
   :style-class "c64-hbox"
   :children
   [{:fx/type     :label
     :text        (format "%-24s" label)
     :style-class "c64-label"}
    (c64-combo items selected-key on-change :disabled disabled)]})

(defn c64-slider
  "Slippery when wet"
  [min max value on-change
   & {:keys [disabled show-value width]
      :or   {disabled   false
             show-value true
             width      200}}]
  {:fx/type     :h-box
   :style-class "c64-hbox"
   :children
   (cond->
    [{:fx/type          :slider
      :style-class      ["slider" "c64-slider"]
      :min              min
      :max              max
      :value            value
      :pref-width       width
      ;;  :block-increment  1 ; ugh
      ;;  :minor-tick-count 0 ; tick
      ;;  :major-tick-unit  5 ; tock
      ;;  :show-tick-labels true ; them labels eh
      ;;  :show-tick-marks  true ; them vertical dashes
      ;;  :snap-to-ticks    true ; makes it stutter
      :on-value-changed on-change
      :disable          disabled}]
     show-value
     (conj
      {:fx/type     :label
       :text        (format "%3s" value)
       :style-class "c64-label-value"
       :min-width   36}))})

(defn c64-labeled-slider
  "Sliding into your dm"
  [label min max value on-change
   & {:keys [disabled]
      :or   {disabled false}}]
  {:fx/type     :h-box
   :style-class "c64-hbox"
   :children
   [{:fx/type     :label
     :text        (format "%20s" label)
     :style-class "c64-label"}
    (c64-slider min max value on-change :disabled disabled)]})

(defn c64-separator
  "Separate the good from the bad"
  []
  {:fx/type     :separator
   :style-class "c64-separator"})

(defn c64-row
  "Two-column label+widget row."
  [label widget]
  {:fx/type     :h-box
   :style-class "c64-hbox"
   :children
   [{:fx/type     :label
     :text        (format "%-20s" label)
     :style-class "c64-label"
     :wrap-text   false
     :min-width   200}
    widget]})

(defn c64-narrow-row
  "Two-column label+widget row."
  [label widget]
  {:fx/type     :h-box
   :style-class "c64-hbox"
   :children
   [{:fx/type     :label
     :text        (format "%-10s" label)
     :style-class "c64-label"
     :wrap-text   false
     :min-width   200}
    widget]})

(defn c64-smol-row
  "Two-column label+widget row."
  [label widget
   & {:keys [min-width]
      :or {min-width 10}}]
  {:fx/type     :h-box
   :style-class "c64-hbox"
   :children
   [{:fx/type     :label
     :text        label
     :style-class "c64-label"
     :wrap-text   false
     :min-width   min-width}
    widget]})
