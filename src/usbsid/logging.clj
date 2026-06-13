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


(defn start-logger
  "Read logging.properties from classpath into the global LogManager.
   Idempotent — safe to call multiple times."
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
