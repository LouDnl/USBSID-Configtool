(ns usbsid.ui.main
  "Head view, on top, big boss"
  (:require
   [cljfx.api :as fx]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [usbsid.state :refer [log!]]
   [usbsid.ui.css :as css]
   [usbsid.ui.sections.common :as common]
   [usbsid.ui.sections.sockets :as s-sockets]
   [usbsid.ui.sections.clock :as s-clock]
   [usbsid.ui.sections.leds :as s-leds]
   [usbsid.ui.sections.features :as s-features]
   [usbsid.ui.sections.sid-tests :as s-tests]
   [usbsid.ui.sections.debug :as s-debug]
   [usbsid.ui.sections.welcome :as s-welcome]
   [usbsid.window-prefs :as w-prefs])
  (:import
   [javafx.application Platform]
   [javafx.beans.value ChangeListener]
   [javafx.scene.image Image]))


;;; State startup/shutdown refs

(def signalled (promise))
(def stopped   (promise))


;;; Here be stuffs

(def nav-items
  [{:key :welcome   :label "WELCOME"}
   {:key :sockets   :label "SOCKETS"}
   {:key :clock     :label "CLOCK"}
   {:key :leds      :label "LEDS"}
   {:key :features  :label "FEATURES"}
   {:key :sid-tests :label "SID TESTS"}
   {:key :debug     :label "DEBUG"}])

(defn- logo-image
  "Well what do you think it is?"
  []
  (try (Image. (str (io/resource "usbsid-configtool-logo.png")))
       (catch Exception _ nil)))

(defn- icon-image
  "It's so smol!"
  []
  (try (Image. (str (io/resource "usbsid-configtool-icon-flat.png")))
       (catch Exception _ nil)))


;;; Here be what you see

(defn title-bar
  "Title shmitle, who cares!?"
  [{:keys [connection]}]
  (let [connected? (= :connected (:status connection))
        cfgstatus  (:config-status connection)
        fw-ver     (:fw-version connection)
        pcb-ver    (:pcb-version connection)]
    {:fx/type     :border-pane
     :style-class "c64-title-bar"
     :left
     {:fx/type  :h-box
      :spacing  10
      :alignment :center-left
      :children
      [(if-let [img (logo-image)]
         {:fx/type      :image-view
          :image        img
          :fit-height   40
          :preserve-ratio true}
         {:fx/type :pane})
       {:fx/type  :v-box
        :spacing  2
        :children
        [{:fx/type     :label
          :text        " USBSID-Pico Configuration Tool" ; ISSUE: This is not neatly ligned out yet
          :style-class "c64-title-text"}
         {:fx/type     :label
          :text        (format "  v%s | %s | %s"
                               (string/trim-newline (slurp (io/resource ".version")))
                               (if fw-ver (str "FW: v" fw-ver) "FW: ---")
                               (if pcb-ver (str "PCB: v" pcb-ver) "PCB: ---"))
          :style-class "c64-subtitle-text"}
         {:fx/type     :label
          :alignment   :center-left
          :text        (case cfgstatus
                         :loaded    "  Config loaded!"
                         :confirmed "  Config confirmed!"
                         :written   "  Config written!"
                         :saved     "  Config saved!"
                         :exportini "  Config exported to ini!"
                         :importini "  Config imported from ini!"
                         :applied   "  Config applied!"
                         "")
          :style-class (case cfgstatus
                         :loaded    "c64-config-loaded"
                         :confirmed "c64-config-confirmed"
                         :written   "c64-config-written"
                         :saved     "c64-config-saved"
                         :exportini "c64-config-exported"
                         :importini "c64-config-imported"
                         :applied   "c64-config-applied"
                         "c64-subtitle-text")}]}]}
     :right
     {:fx/type    :v-box
      :spacing    4
      :alignment  :center-right
      :padding    {:top 4 :right 0 :bottom 4 :left 0}
      :children
      [{:fx/type     :label
        :alignment   :center
        :max-width   Double/MAX_VALUE
        :text        (if connected? "** CONNECTED **" "** DISCONNECTED **")
        :style-class (if connected? "c64-status-connected" "c64-status-disconnected")}
       {:fx/type   :h-box
        :spacing   6
        :children
        [{:fx/type     :button
          :text        "CONNECT"
          :on-action   {:event/type :connect}
          :disable     connected?
          :style-class (if connected? "c64-button" "c64-button-primary")}
         {:fx/type     :button
          :text        "DISCONNECT"
          :on-action   {:event/type :disconnect}
          :disable     (not connected?)
          :style-class (if connected? "c64-button-primary" "c64-button")}]}]}}))

(defn warning-banner
  "Well that's what you get for messing with me!"
  []
  {:fx/type   :h-box
   :spacing   12
   :alignment :center
   :padding   {:top    6
               :right  8
               :bottom 6
               :left   8}
   :children
   [{:fx/type     :v-box
     :style-class "c64-warning-banner"
     :alignment   :center
     :spacing     0
     :children
     [{:fx/type     :label
       :text        "!!! CONFIGURATION NEEDS VERIFICATION !!!"
       :style-class ["c64-warning-text" "c64-text-wrap"]}
      {:fx/type     :label
       :text        "!!! SOCKET POWER IS DISABLED UNTIL CONFIRMED !!!"
       :style-class ["c64-warning-text" "c64-text-wrap"]}
      {:fx/type     :button

       :text        "CONFIRM"
       :on-action   {:event/type :confirm-config}
       :wrap-text   true
       :style-class "c64-button-danger"}]}]})

(defn nav-panel
  "We navigate the stars together!"
  [{:keys [active-section]}]
  {:fx/type     :v-box
   :style-class "c64-nav"
   :spacing     0
   :children
   (mapv (fn [{:keys [key label]}]
           {:fx/type     :button
            :text        label
            :on-action   {:event/type :navigate :section key}
            :style-class (if (= key active-section)
                           ["c64-nav-item" "c64-nav-item-active"]
                           "c64-nav-item")})
         nav-items)})

(defn content-panel
  "Look at me, I can see you!"
  [{:keys [active-section config connected? connection hover-popup]}]
  {:fx/type      :scroll-pane
   :style-class  "c64-scroll-pane"
   :fit-to-width true
   :hbar-policy  :never
   :h-box/hgrow  :always
   :content
   (case active-section
     :sockets   (s-sockets/sockets-section {:config config :connection connection :hover-popup hover-popup})
     :clock     (s-clock/clock-section {:config config :connection connection :hover-popup hover-popup})
     :leds      (s-leds/leds-section {:config config :hover-popup hover-popup})
     :features  (s-features/features-section {:config config :connection connection :hover-popup hover-popup})
     :sid-tests (s-tests/sid-tests-section {:connected? connected?})
     :debug     (s-debug/debug-section {:connected? connected?})
     :welcome   (s-welcome/about-section {:connection connection})
     {:fx/type     :label
      :text        "Select a section"
      :style-class "c64-label"})})

(defn- action-buttons
  [connected? fw-line hover-popup]
  (case fw-line
    (:legacy :unknown)
    [(common/buttons :read {:hover-popup hover-popup
                            :connected?  connected?})
     (common/buttons :write {:hover-popup hover-popup
                             :connected?  connected?})
     (common/buttons :apply {:hover-popup hover-popup
                             :connected?  connected?})
     {:fx/type     :pane
      :h-box/hgrow :always}
     (common/buttons :save {:hover-popup hover-popup
                            :connected?  connected?})
     (common/buttons :save-reboot {:hover-popup hover-popup
                                   :connected?  connected?})
     (common/buttons :reload-flash {:hover-popup hover-popup
                                    :connected?  connected?})]
    ; else :v0_7+
    [(common/buttons :read {:hover-popup hover-popup
                            :connected?  connected?})
     {:fx/type     :pane
      :h-box/hgrow :always}
     (common/buttons :save {:hover-popup hover-popup
                            :connected?  connected?})
     (common/buttons :save-reboot {:hover-popup hover-popup
                                   :connected?  connected?})
     (common/buttons :reload-flash {:hover-popup hover-popup
                                    :connected?  connected?})]))

(defn action-bar
  "Are you getting any!?"
  [{:keys [connected? fw-line hover-popup]}]
  {:fx/type :v-box
   :spacing 0
   :padding {:top    3
             :right  0
             :bottom 3
             :left   0}
   :children
   [{:fx/type     :h-box
     :style-class "c64-hbox"
     :padding     {:top    4
                   :right  8
                   :bottom 2
                   :left   8}
     :spacing     6
     :children (action-buttons connected? fw-line hover-popup)}
    {:fx/type     :h-box
     :style-class "c64-hbox"
     :padding     {:top    2
                   :right  8
                   :bottom 4
                   :left   8}
     :spacing     6
     :children
     [(common/buttons :save-ini {:hover-popup hover-popup})
      (common/buttons :load-ini {:hover-popup hover-popup})
      {:fx/type     :pane
       :h-box/hgrow :always}
      (common/buttons :reset-dflts {:hover-popup hover-popup
                                    :connected? connected?})]}]})

(defn- log-text-area
  "Here be letters!"
  [{:keys [log]}]
  {:fx/type    fx/ext-on-instance-lifecycle
   :on-created (fn [^javafx.scene.control.TextArea ta]
                 (.addListener
                  (.textProperty ta)
                  (reify ChangeListener
                    (changed [_ _ _ new-text]
                      (Platform/runLater
                       (fn [] (.positionCaret ta (count new-text))))))))
   :desc       {:fx/type     :text-area
                :style-class "c64-log-area"
                :text        (string/join "\n" log)
                :editable    false
                :wrap-text   false
                :pref-height 100
                :min-height  80}})

(defn log-panel
  "It's where the leprechauns hide"
  [{:keys [log]}]
  {:fx/type     :v-box
   :style-class "c64-log-bar"
   :spacing     0
   :children
   [{:fx/type     :label
     :padding     {:top    3
                   :right  0
                   :bottom 3
                   :left   0}
     :text        "LOG:"
     :style-class "c64-label-dim"}
    (log-text-area {:log log})]})

(defn root-view
  "In your face!"
  [{:keys [connection config active-section log hover-popup]
    :as _state}]
  (let [connected?    (= :connected (:status connection))
        need-confirm? (get config :need-confirmation false)]
    {:fx/type  :stage
     :showing  true
     :title    "USBSID-Pico Config Tool"
     :on-close-request (fn [_]
                         #_(.fine @logging/logger "> Application is set to close")
                         (log! "Application is set to close")
                         (w-prefs/save-window-state)
                         (deliver signalled true))
     :on-hidden (fn [_]
                  #_(.fine @logging/logger "> Application closed")
                  (log! "Application closed")
                  (w-prefs/save-window-state)
                  (deliver stopped true))
     ;;  :min-width  900
     ;;  :min-height 650
     ;;  :width      1280
     ;;  :height     1024
     :min-width  1024
     :min-height 768
     :icons (let [img (icon-image)] (if img [img] []))
     :scene
     {:fx/type     :scene
      :stylesheets css/stylesheets
      :root
      {:fx/type     :border-pane
       :style-class "c64-scene-root"
       :top
       {:fx/type  :v-box
        :spacing  0
        :children (cond-> [(title-bar {:connection connection})]
                    (and
                     need-confirm?
                     connected?) (conj (warning-banner)))}
       :center
       {:fx/type  :h-box
        :children [(nav-panel {:active-section active-section})
                   (content-panel {:active-section active-section
                                   :config         config
                                   :connected?     connected?
                                   :connection     connection
                                   :hover-popup    hover-popup})]}
       :bottom
       {:fx/type  :v-box
        :spacing  0
        :children [(action-bar {:connected?  connected?
                                :fw-line     (:fw-line connection)
                                :hover-popup hover-popup})
                   (log-panel {:log log})]}}}}))
