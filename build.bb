#!/usr/bin/env bb
;; USBSID-Pico is a RPi Pico (RP2040/RP2350) based board for interfacing one
;; or two MOS SID chips and/or hardware SID emulators over (WEB)USB with your
;; computer, phone or ASID supporting player.
;;
;; build.bb (Configtool build)
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

(require
 '[babashka.cli :as cli]
 '[babashka.process :refer [shell]]
 '[clojure.string :refer [starts-with? trim-newline]])

(def readers
  ; Add readers for handling unknown aero tags in config.edn
  {:readers (merge default-data-readers
                   *data-readers*
                   {'object   #(str %)
                    'function #(str %)
                    'env      #(str %)
                    'or       #(str %)
                    'ref      #(str %)
                    'join     #(str %)})})

(def version
  ; Retrieve current version string from config
  (trim-newline (slurp "resources/.version")))

(when
 ; Exit prematurely with an error if version string starts with a 'v'
 (starts-with? version "v")
  (println (format "Error version string '%s' is incorrect, exiting!" version))
  (System/exit 1))

(def spec
  {:uber         {:ref   "uberjar file"
                  :alias :u
                  :desc  "builds a deployable uberjar file (runs a clean first)"}
   :package      {:ref   "executable package"
                  :alias :p
                  :desc  "Build a self-contained fat JAR including all deps + JavaFX platform JARs"}
   :ci           {:ref   "full CI pipeline"
                  :alias :ci
                  :desc  "Run full CI pipeline: tests + uberjar + launcher scripts"}
   :tests        {:ref   "unittests"
                  :alias :t
                  :desc  "Run all unittests"}
   :release      {:ref   "executable package"
                  :alias :r
                  :desc  "Full release build: CI pipeline + platform package"}})

(def order
  [:uber :package :ci :tests :release])

(let [args    (cli/parse-opts *command-line-args* {:spec spec})
      help    (cli/format-opts {:spec  spec
                                :order order})
      arg     (-> args keys first)
      job     (if (contains? spec arg)  ;; lets assume we will only get 1 command
                (name arg)
                (do
                  (println args "<- not a correct job")
                  (println)
                  (println "Usage:")
                  (println help)
                  (System/exit 1)))
      command "clojure -T:build %s"]  ;; replaces kaocha.sh, uses native clojure instead of clj which requires rlwrap
  (try
    (condp = (count args)
      1 (shell (format command job))
      (do
        (println args "<- not a correct job or too many arguments")
        (println)
        (println "Usage:")
        (println help)))
    (finally
      (System/exit 0))))
