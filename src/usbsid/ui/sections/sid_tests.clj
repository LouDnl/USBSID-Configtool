(ns usbsid.ui.sections.sid-tests
  "I can make it go beep!"
  (:require
   [usbsid.ui.widgets :as w]))

(defn sid-tests-section
  "Well, it is what is is now aint it?"
  [{:keys [connected?]}]
  {:fx/type     :v-box
   :style-class "c64-vbox"
   :spacing     10
   :padding     {:top    8
                 :right  8
                 :bottom 8
                 :left   8}
   :children
   [(w/c64-header "DETECTION")

    {:fx/type :h-box
     :spacing 12
     :children
     [{:fx/type     :v-box
       :style-class "c64-section"
       :spacing     6
       :children
       [{:fx/type     :label
         :text        "Auto detect will run a full chip and SID type detection on both sockets. Then saves and applies the configuration, no reboot needed."
         :style-class ["c64-label-dim" "c64-text-wrap"]}
        {:fx/type     :button
         :text        "START AUTO DETECT"
         :on-action   {:event/type :auto-detect}
         :disable     (not connected?)
         :style-class "c64-button-primary"}
        (w/c64-separator)
        {:fx/type     :label
         :text        "Run Chip or SID tests independently, output of these tests is set in the USBSID config but not applied, it is also printed via the uart debug port."
         :style-class "c64-label-dim"}
        {:fx/type     :h-box
         :style-class "c64-hbox"
         :spacing     6
         :children
         [{:fx/type     :button
           :text        "DETECT SIDS"
           :on-action   {:event/type :detect-sids}
           :disable     (not connected?)
           :style-class "c64-button"}
          {:fx/type     :button
           :text        "DETECT CLONES"
           :on-action   {:event/type :detect-clones}
           :disable     (not connected?)
           :style-class "c64-button"}]}]}]}

    (w/c64-separator)
    (w/c64-header "SID TESTS")

    {:fx/type :h-box
     :spacing 12
     :children
     [{:fx/type     :v-box
       :style-class "c64-section"
       :spacing     6
       :children
       [{:fx/type     :label
         :text        "These buttons start audio testing of SID('s) by testing each waveform/filter combination etc."
         :style-class ["c64-label-dim" "c64-text-wrap"]}

        {:fx/type     :h-box
         :style-class "c64-hbox"
         :spacing     6
         :children
         [{:fx/type     :button
           :text        "TEST SID 1"
           :on-action   {:event/type :test-sid
                         :n          1}
           :disable     (not connected?)
           :style-class "c64-button"}
          {:fx/type     :button
           :text        "TEST SID 2"
           :on-action   {:event/type :test-sid
                         :n          2}
           :disable     (not connected?)
           :style-class "c64-button"}
          {:fx/type     :button
           :text        "TEST SID 3"
           :on-action   {:event/type :test-sid
                         :n          3}
           :disable     (not connected?)
           :style-class "c64-button"}
          {:fx/type     :button
           :text        "TEST SID 4"
           :on-action   {:event/type :test-sid
                         :n          4}
           :disable     (not connected?)
           :style-class "c64-button"}]}

        {:fx/type     :h-box
         :style-class "c64-hbox"
         :spacing     6
         :children
         [{:fx/type     :button
           :text        "TEST ALL"
           :on-action   {:event/type :test-all-sids}
           :disable     (not connected?)
           :style-class "c64-button"}
          {:fx/type     :button
           :text        "STOP TESTS"
           :on-action   {:event/type :stop-tests}
           :disable     (not connected?)
           :style-class "c64-button-danger"}]}

        {:fx/type     :label
         :text        "Tests can run on the board for a long time. Use STOP TESTS to interrupt."
         :style-class ["c64-label-dim" "c64-text-wrap"]}]}]}]})
