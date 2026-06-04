(ns usbsid.logging
  "Ping, pong, poops in your cli"
  (:import
   [java.util.logging Logger LogManager]))

(def logger (promise))

(defn start-logger
  "Start it, or not"
  []
  (-> (LogManager/getLogManager)
      (.readConfiguration
       (.getResourceAsStream
        (ClassLoader/getSystemClassLoader)
        "logging.properties")))
  (System/setProperty "java.util.logging.SimpleFormatter.format"
                      "[%1$tF %1$tT] [%4$s] [CONFIG] %5$s %n");
  (deliver logger (Logger/getLogger (.getName 'usbsid.core))))
