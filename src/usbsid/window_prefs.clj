(ns usbsid.window-prefs
  "Save Window size an location, because I can :P"
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io])
  (:import
   [javafx.stage Window]))


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


;;; Save and restore the state

(defn set-window-state
  "Set the window state"
  []
  (let [{:keys [x y w h]} (load-window)]
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
