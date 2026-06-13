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
