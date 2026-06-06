(ns usbsid.core
  "The core of all things that never were"
  (:require
   [cljfx.api :as fx]
   [usbsid.driver :as driver]
   [usbsid.events :as events]
   [usbsid.logging :as logging :refer [logger]]
   [usbsid.state :as state]
   [usbsid.ui.main :as ui]
   [usbsid.window-prefs :as win-prefs])
  (:import
   [java.util.logging Level]
   [javafx.application Platform]
   [javafx.stage Window])
  (:gen-class))


(defn ui-map-desc
  "Default ui map description"
  [s]
  {:fx/type        ui/root-view
   :connection     (:connection s)
   :config         (:config s)
   :active-section (:active-section s)
   :log            (:log s)
   :dirty          (:dirty s)})

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
    (.fine @logger "Driver disconnected"))
  (when @renderer
    (fx/unmount-renderer state/*state @renderer)
    (reset! renderer nil)
    (reset! state/*state state/initial-state)
    (.fine @logger "Renderer & state unmounted"))
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
    (.fine @logger "Renderer & state mounted")
    (deref ui/signalled)
    (.fine @logger "Signal to shutdown received")
    (finally
      (try
        (deref ui/stopped)
        (stop-app)
        (catch Exception e
          (.log @logger Level/SEVERE "Error during shutdown" e)))
      (Platform/exit)
      (shutdown-agents)
      (.fine @logger "All agents shutdown")
      (System/exit 0))))
