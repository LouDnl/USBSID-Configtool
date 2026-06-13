#!/usr/bin/env bb

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
