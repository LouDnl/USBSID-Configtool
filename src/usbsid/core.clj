;; USBSID-Pico is a RPi Pico (RP2040/RP2350) based board for interfacing one
;; or two MOS SID chips and/or hardware SID emulators over (WEB)USB with your
;; computer, phone or ASID supporting player.
;;
;; core.clj (Configtool)
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

(ns usbsid.core
  "The core of all things that never were"
  (:require
   [cljfx.api :as fx]
   [usbsid.driver :as driver]
   [usbsid.events :as events]
   [usbsid.logging :as logging]
   [usbsid.state :as state]
   [usbsid.ui.css :as css]
   [usbsid.ui.main :as ui]
   [usbsid.window-prefs :as win-prefs])
  (:import
   [javafx.application Platform]
   [javafx.stage Window])
  (:gen-class))


;;; Well, get into it already!

(defn ui-map-desc
  "Default ui map description"
  [s]
  {:fx/type        ui/root-view
   :connection     (:connection s)
   :config         (:config s)
   :active-section (:active-section s)
   :log            (:log s)
   :dirty          (:dirty s)
   :app-nonce      (:app-nonce s 0)
   :hover-popup    (:hover-popup s)})

(def renderer (atom nil))

(defn new-renderer
  "Create a new renderer"
  []
  (fx/create-renderer
   :middleware (fx/wrap-map-desc ui-map-desc)
   :opts {:fx.opt/map-event-handler events/handle}))

(defn start-app
  "Start the application"
  []
  (logging/start-logger)
  (css/load-fonts!)
  (Platform/setImplicitExit false)
  (reset! renderer (new-renderer))
  (fx/mount-renderer state/*state @renderer)
  (Platform/runLater
   (fn []
     (win-prefs/set-window-state))))

(defn stop-app
  "Stop the application"
  []
  (when (driver/connected?)
    (driver/disconnect!)
    (logging/fine "Driver disconnected"))
  (when @renderer
    (fx/unmount-renderer state/*state @renderer)
    (reset! renderer nil)
    (reset! state/*state state/initial-state)
    (logging/fine "Renderer & state unmounted"))
  (let [done (promise)]
    (Platform/runLater
     (fn []
       (win-prefs/save-window-state)
       (doseq [^Window w (into [] (Window/getWindows))]
         (.hide w))
       (deliver done true)))
    @done))

(defn -main
  "Application entrypoint"
  [& _args]
  (try
    (start-app)
    (logging/fine "Renderer & state mounted")
    (deref ui/signalled)
    (logging/fine "Signal to shutdown received")
    (finally
      (try
        (deref ui/stopped)
        (stop-app)
        (catch Exception e
          (logging/severe "Error during shutdown" e)))
      (Platform/exit)
      (shutdown-agents)
      (logging/fine "All agents shutdown")
      (System/exit 0))))
