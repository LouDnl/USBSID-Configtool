(ns usbsid.window-prefs
  "Save Window size an location, because I can :P"
  (:import
   [java.util.prefs Preferences]
   [javafx.stage Window]))


; This is, well if you use Linux, located at ~/.java/.userPrefs/usbsid-pico-configtool/prefs.xml
(def ^:private ^Preferences node
  (.node (Preferences/userRoot) "usbsid-pico-configtool"))

(defn load-window
  "Load window settings from node"
  []
  {:x (.getDouble node "x" Double/NaN) ; no default window position
   :y (.getDouble node "y" Double/NaN) ; no default window position
   :w (.getDouble node "w" 1280.0)     ; default width
   :h (.getDouble node "h" 1024.0)})   ; default height

(defn save-window!
  "Save window settings to node"
  [{:keys [x y w h]}]
  (.putDouble node "x" x)
  (.putDouble node "y" y)
  (.putDouble node "w" w)
  (.putDouble node "h" h)
  (try (.flush node) (catch Exception _)))

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
     {:x (.getX win) :y (.getY win)
      :w (.getWidth win) :h (.getHeight win)})))
