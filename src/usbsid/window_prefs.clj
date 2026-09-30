;; USBSID-Pico is a RPi Pico (RP2040/RP2350) based board for interfacing one
;; or two MOS SID chips and/or hardware SID emulators over (WEB)USB with your
;; computer, phone or ASID supporting player.
;;
;; window_prefs.clj (Configtool)
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

(ns usbsid.window-prefs
  "Save Window size an location, because I can :P"
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io])
  (:import
   [javafx.stage Screen Window]))


;;; Directory & file location

(defn config-dir
  []
  (let [home (System/getProperty "user.home")
        os   (System/getProperty "os.name" "")]
    (cond
      (.startsWith os "Windows")
      (io/file (or (System/getenv "APPDATA") home) "usbsid-configtool")

      (.startsWith os "Mac")
      (io/file home "Library" "Application Support" "usbsid-configtool")

      :else
      (io/file (or (System/getenv "XDG_CONFIG_HOME")
                   (str home "/.config"))
               "usbsid-configtool"))))

(defn- prefs-file
  []
  (io/file (config-dir) "prefs.edn"))


;;; Default window size

(def ^:private defaults
  {:x Double/NaN
   :y Double/NaN
   :w 1280.0
   :h 1024.0})


;;; Internal wrappers

(defn- load-window
  "Load window settings from ~/.config/usbsid-configtool/prefs.edn"
  []
  (try
    (if (.exists (prefs-file))
      (merge defaults (edn/read-string (slurp (prefs-file))))
      defaults)
    (catch Exception _ defaults)))

(defn- save-window!
  "Save window settings to ~/.config/usbsid-configtool/prefs.edn"
  [{:keys [x y w h]}]
  (try
    (io/make-parents (prefs-file))
    (spit (prefs-file) (pr-str {:x x :y y :w w :h h}))
    (catch Exception _)))


;;; Screen fit

(defn fit-to-screen
  "Clamp a width and height to the primary screen's visual bounds.
   Call on the JavaFX application thread."
  [w h]
  (let [bounds (.getVisualBounds (Screen/getPrimary))]
    [(min w (.getWidth bounds))
     (min h (.getHeight bounds))]))


;;; Save and restore the state

(defn set-window-state
  "Set the window state"
  []
  (let [{:keys [x y w h]} (load-window)
        [w h]             (fit-to-screen w h)]
    (when-let [^Window win (first (Window/getWindows))]
      (.setWidth  win w)
      (.setHeight win h)
      (when-not (Double/isNaN x) (.setX win x))
      (when-not (Double/isNaN y) (.setY win y)))))

(defn save-window-state
  "Save the window state"
  []
  (when-let [^Window win (first (Window/getWindows))]
    (save-window!
     {:x (.getX win)
      :y (.getY win)
      :w (.getWidth win)
      :h (.getHeight win)})))
