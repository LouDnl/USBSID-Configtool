;; USBSID-Pico is a RPi Pico (RP2040/RP2350) based board for interfacing one
;; or two MOS SID chips and/or hardware SID emulators over (WEB)USB with your
;; computer, phone or ASID supporting player.
;;
;; logging.clj (Configtool)
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

(ns usbsid.logging
  "java.util.logging setup + per-namespace logger helpers + line-capturing log macros.

   Pattern: each call site uses one of the level macros (`fine` / `info` /
   `warning` / `severe`). The macro expands with `*ns*` and `(:line (meta &form))`
   resolved at compile time, then calls the corresponding `<level>!` fn which
   builds a `LogRecord`, sets `loggerName` + `sourceClassName` to the namespace
   and `sourceMethodName` to `\"L<line>\"`. The `%2$s` format slot in
   `resources/logging.properties` then renders `\"<ns> L<line>\"`."
  (:import
   [java.util.logging Level Logger LogManager LogRecord]))


;;; Well well well

(defn start-logger
  "Read logging.properties from classpath into the global LogManager.
   Idempotent - safe to call multiple times."
  []
  (-> (LogManager/getLogManager)
      (.readConfiguration
       (.getResourceAsStream
        (ClassLoader/getSystemClassLoader)
        "logging.properties"))))

(defn get-logger
  "Runtime lookup. Pass a symbol/keyword/string naming the namespace.
   ```clojure
     (.info (usbsid.logging/get-logger *ns*) \"my message\")
   ```"
  ^Logger [ns-name]
  (Logger/getLogger (str ns-name)))

(defmacro logger
  "Expands at the call site to (Logger/getLogger \"<calling-ns>\").
   Use at the top of a namespace:
   ```clojure
     (def ^:private logger (logging/logger))
   ```
   so that `(.info logger msg)` etc. log under the consumer's own name."
  []
  `(Logger/getLogger ~(str *ns*)))


;;; Line-capturing log helpers

(defn- emit!
  "Build a LogRecord with caller ns + line stashed into sourceClassName so
   SimpleFormatter `%2$s` shows `\"<ns>:<line>\"`. When `method` is a non-blank
   string, append it via `setSourceMethodName` (renders as `\" <method>\"`
   trailing the source slot). When nil/blank, the slot stays unset so JUL
   doesn't emit a leading space. Optional Throwable attached via setThrown."
  ([^Logger lg ^Level level ^String ns-str line ^String msg]
   (emit! lg level ns-str line msg nil nil))
  ([^Logger lg ^Level level ^String ns-str line ^String msg ^Throwable t]
   (emit! lg level ns-str line msg t nil))
  ([^Logger lg ^Level level ^String ns-str line ^String msg ^Throwable t ^String method]
   (let [rec (LogRecord. level msg)]
     (.setLoggerName rec ns-str)
     (.setSourceClassName rec (str ns-str ":" line))
     (when (and method (not (.isBlank method)))
       (.setSourceMethodName rec method))
     (when t (.setThrown rec t))
     (.log lg rec))))

(defn fine!
  ([ns-str line msg]        (emit! (Logger/getLogger ns-str) Level/FINE    ns-str line msg nil nil))
  ([ns-str line msg method] (emit! (Logger/getLogger ns-str) Level/FINE    ns-str line msg nil method)))
(defn info!
  ([ns-str line msg]        (emit! (Logger/getLogger ns-str) Level/INFO    ns-str line msg nil nil))
  ([ns-str line msg method] (emit! (Logger/getLogger ns-str) Level/INFO    ns-str line msg nil method)))
(defn warning!
  ([ns-str line msg]        (emit! (Logger/getLogger ns-str) Level/WARNING ns-str line msg nil nil))
  ([ns-str line msg method] (emit! (Logger/getLogger ns-str) Level/WARNING ns-str line msg nil method)))
(defn severe!
  ([ns-str line msg]          (emit! (Logger/getLogger ns-str) Level/SEVERE ns-str line msg nil nil))
  ([ns-str line msg t]        (emit! (Logger/getLogger ns-str) Level/SEVERE ns-str line msg t   nil))
  ([ns-str line msg t method] (emit! (Logger/getLogger ns-str) Level/SEVERE ns-str line msg t   method)))

(defmacro fine
  "Log at FINE level with caller ns/line auto-captured."
  [msg]
  `(fine! ~(str *ns*) ~(:line (meta &form)) ~msg))

(defmacro info
  "Log at INFO level with caller ns/line auto-captured."
  [msg]
  `(info! ~(str *ns*) ~(:line (meta &form)) ~msg))

(defmacro warning
  "Log at WARNING level with caller ns/line auto-captured."
  [msg]
  `(warning! ~(str *ns*) ~(:line (meta &form)) ~msg))

(defmacro severe
  "Log at SEVERE level with caller ns/line auto-captured. Optional Throwable."
  ([msg]
   `(severe! ~(str *ns*) ~(:line (meta &form)) ~msg))
  ([msg t]
   `(severe! ~(str *ns*) ~(:line (meta &form)) ~msg ~t)))
