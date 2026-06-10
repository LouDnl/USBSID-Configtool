(ns build
  (:refer-clojure :exclude [test])
  (:require
   [clojure.tools.build.api :as b]
   [clojure.string :as string])
  (:import
   [java.io File]))

(def lib        'org.clojars.loud/usbsid-configtool)
(def version    (string/trim-newline (slurp "resources/.version")))
(def main       'usbsid.core)
(def app-name   "USBSID-Pico-Configtool")
(def class-dir  "target/classes")
(def uber-file  (format "target/%s-%s.jar" (name lib) version))

(def ^:private driver-group    "usbsid")
(def ^:private driver-artifact "usbsid-usb-driver-library-java")
(def ^:private driver-version  "1.1")

(defn- ensure-driver!
  "Install the USBSID driver JAR/POM into the local Maven repo from assets/driver/ if absent."
  []
  (let [sep        File/separator
        m2-repo    (str (System/getProperty "user.home") sep ".m2" sep "repository")
        target-dir (File. (str m2-repo sep driver-group sep driver-artifact sep driver-version))
        jar-name   (str driver-artifact "-" driver-version ".jar")
        pom-name   (str driver-artifact "-" driver-version ".pom")
        target-jar (File. target-dir jar-name)]
    (when-not (.exists target-jar)
      (let [src-jar (File. (str "assets" sep "driver" sep jar-name))
            src-pom (File. (str "assets" sep "driver" sep pom-name))]
        (when-not (.exists src-jar)
          (throw (ex-info "Driver JAR not found in local Maven repo or assets/driver/"
                          {:looked-for (.getAbsolutePath src-jar)})))
        (println "\nDriver not in local Maven repo — installing from assets/driver/...")
        (.mkdirs target-dir)
        (b/copy-file {:src (.getPath src-jar) :target (.getPath (File. target-dir jar-name))})
        (when (.exists src-pom)
          (b/copy-file {:src (.getPath src-pom) :target (.getPath (File. target-dir pom-name))}))
        (println (str "  Installed " jar-name " -> " (.getAbsolutePath target-dir)))))))

; JVM args for JavaFX 11+ running from a fat jar (non-modular classpath mode)
(def jfx-opts
  ["--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED"
   "--add-opens=javafx.base/com.sun.javafx.runtime=ALL-UNNAMED"
   "--add-opens=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED"])
(def java-opts
  ["--enable-native-access=ALL-UNNAMED"
   "--sun-misc-unsafe-memory-access=allow"]) ; JVM 22+
(def compile-opts
  (conj java-opts "-Dcljfx.skip-javafx-initialization=true"))
(def jfx-jvm-args
  (string/join " " (into jfx-opts java-opts)))
(def jpackage-opts
  (into jfx-opts java-opts))

; test

(defn test
  "Run all tests via cognitect test-runner."
  [opts]
  (ensure-driver!)
  (let [basis    (b/create-basis {:aliases [:test]})
        cmds     (b/java-command
                  {:basis     basis
                   :main      'clojure.main
                   :main-args ["-m" "cognitect.test-runner"]})
        {:keys [exit]} (b/process cmds)]
    (when-not (zero? exit) (throw (ex-info "Tests failed" {}))))
  opts)

; uber

(defn uber
  "Build a self-contained fat JAR including all deps + JavaFX platform JARs."
  [opts]
  (ensure-driver!)
  (b/delete {:path "target"})
  (let [basis (b/create-basis {})]
    (println "\nCopying sources and resources...")
    (b/copy-dir {:src-dirs ["src" "resources"] :target-dir class-dir})
    (println (str "\nCompiling " main "..."))
    (b/compile-clj {:basis      basis
                    :src-dirs   ["src"]
                    :class-dir  class-dir
                    :ns-compile [main]
                    :java-opts  compile-opts})
    (println "\nBuilding uberjar...")
    (let [skip (fn [_] nil)]  ; skip duplicate - return nil = no write
      (b/uber {:class-dir class-dir
               :uber-file uber-file
               :basis     basis
               :main      main
               :conflict-handlers
               {"^module-info.class$"    skip
                "^META-INF/MANIFEST.MF$" skip
                "^META-INF/maven/.*"     skip
                "^META-INF/services/.*"  :append
                "^META-INF/.*"           skip}}))
    (println (str "\nBuilt: " uber-file " ("
                  (int (/ (.length (File. uber-file)) 1024 1024))
                  " MB)")))
  opts)

; launcher scripts

(defn- write-launchers []
  (let [jar   (str (name lib) "-" version ".jar")
        sh    (str "#!/bin/sh\n"
                   "DIR=\"$(cd \"$(dirname \"$0\")\" && pwd)\"\n"
                   "exec java " jfx-jvm-args " -jar \"$DIR/" jar "\" \"$@\"\n")
        bat   (str "@echo off\r\n"
                   "set DIR=%~dp0\r\n"
                   "java " (string/replace jfx-jvm-args "\n" " ") " -jar \"%DIR%" jar "\" %*\r\n")]
    (spit "target/run.sh"  sh)
    (spit "target/run.bat" bat)
    (.setExecutable (File. "target/run.sh") true)
    (println "  Wrote target/run.sh and target/run.bat")))

; jpackage

(defn- detect-pkg-type []
  (let [os (string/lower-case (System/getProperty "os.name"))]
    (cond
      (string/includes? os "linux")   "app-image"
      (string/includes? os "windows") "msi"
      (string/includes? os "mac")     "dmg"
      :else                           "app-image")))

; ISSUE:

;; WARNING: A restricted method in java.lang.System has been called
;; WARNING: java.lang.System::load has been called by com.sun.glass.utils.NativeLibLoader in an unnamed module (file:/mnt/loud/Code/Development/pi/USBSID-Pico-dev/configtool-repo/target/package/USBSID-Pico-Configtool/lib/app/usbsid-configtool-0.1.0.jar)
;; WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
;; WARNING: Restricted methods will be blocked in a future release unless native access is enabled

;; WARNING: A terminally deprecated method in sun.misc.Unsafe has been called
;; WARNING: sun.misc.Unsafe::allocateMemory has been called by com.sun.marlin.OffHeapArray (file:/mnt/loud/Code/Development/pi/USBSID-Pico-dev/configtool-repo/target/package/USBSID-Pico-Configtool/lib/app/usbsid-configtool-0.1.0.jar)
;; WARNING: Please consider reporting this to the maintainers of class com.sun.marlin.OffHeapArray
;; WARNING: sun.misc.Unsafe::allocateMemory will be removed in a future release


(defn package
  "Create a platform-native package using jpackage.
  On Linux: app-image (self-contained directory).
  On Windows: msi installer (requires WiX toolset), per-user install scope.
  On macOS: dmg image.
  NOTE: cross-compilation not supported - run on each target platform."
  [opts]
  (when-not (.exists (File. uber-file))
    (uber opts))
  (let [pkg-type  (or (:type opts) (detect-pkg-type))
        jar-name  (str (name lib) "-" version ".jar")
        input-dir "target/jpackage-input"
        dest      "target/package"
        ; macOS jpackage requires integers only, first component > 0.
        ; On macOS only: bump leading 0 to 1 (0.1.0 → 1.1.0) in package metadata.
        ver       (let [v     (string/replace version "-SNAPSHOT" "")
                        macos (string/includes? (string/lower-case (System/getProperty "os.name")) "mac")
                        parts (string/split v #"\." 3)]
                    (if (and macos (= "0" (first parts)))
                      (string/join "." (cons "1" (rest parts)))
                      v))]
    ; Copy only the JAR into a clean input dir (avoids jpackage recursing into dest)
    (b/delete {:path input-dir})
    (b/delete {:path dest})
    (.mkdirs (File. input-dir))
    (.mkdirs (File. dest))
    (b/copy-file {:src uber-file :target (str input-dir "/" jar-name)})
    (println (str "\nPackaging as " pkg-type " -> " dest "/..."))
    (let [os           (string/lower-case (System/getProperty "os.name"))
          win?         (string/includes? os "windows")
          mac?         (string/includes? os "mac")
          icon         (cond
                         win?                          "resources/usbsid-configtool-icon.ico"
                         (string/includes? os "linux") "resources/usbsid-configtool-icon-flat.png"
                         mac?                          "resources/usbsid-configtool-icon.icns"
                         :else                         nil)
          ; macOS hardened-runtime (jpackage default) blocks System.load of unsigned
          ; libusb4java.dylib extracted from classifier JAR. Entitlements disable
          ; library validation so the JNI native loads. Without these the app hangs at
          ; UsbHostManager.getUsbServices() with a silent UnsatisfiedLinkError.
          entitlements "resources/mac-entitlements.plist"
          {:keys [exit]}
          (b/process
           {:command-args
            (cond-> (into
                     ["jpackage"
                      "--type"           pkg-type
                      "--name"           app-name
                      "--app-version"    ver
                      "--input"          input-dir
                      "--main-jar"       jar-name
                      "--main-class"     "usbsid.core"
                      "--dest"           dest
                      "--vendor"         "LouD"
                      "--description"    "USBSID-Pico Configuration Tool"
                      "--copyright"      "Copyright 2024-2026 LouD, GPLv2"]
                     (mapcat #(vector "--java-options" %) jpackage-opts))
              (and icon (.exists (File. ^String icon))) (into ["--icon" icon])
              (and mac? (.exists (File. ^String entitlements)))
              (into ["--mac-entitlements" entitlements])
              win? (into ["--win-menu"
                          "--win-menu-group"    "USBSID-Pico"
                          "--win-shortcut"
                          "--win-dir-chooser"
                          "--win-per-user-install"
                          "--win-upgrade-uuid"  "b2e8c4f1-3a5d-4e6b-9c7d-0f1e2a3b4c5d"]))})]
      (if (zero? exit)
        (println (str "Package created in " dest "/"))
        (throw (ex-info "jpackage failed" {:exit exit})))))
  opts)

; ci

(defn ci
  "Run full CI pipeline: tests + uberjar + launcher scripts."
  [opts]
  (test opts)
  (uber opts)
  (write-launchers)
  opts)

(defn release
  "Full release build: CI pipeline + platform package."
  [opts]
  (ci opts)
  (package opts)
  opts)
