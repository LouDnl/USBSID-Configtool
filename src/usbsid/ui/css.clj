(ns usbsid.ui.css
  "I make things pritteh!"
  (:require
   [clojure.java.io :as io]
   [cljfx.css :as css])
  (:import
   [javafx.scene.text Font]))


;;; Font pre-loading - Font/loadFont handles jar: URLs; CSS @font-face alone cannot

(defn load-fonts!
  "Pre-load custom fonts so JavaFX can find them by family name.
   Must be called after FX toolkit is initialized, before first render."
  []
  (doseq [name ["C64_Pro_Mono-STYLE.ttf" "C64_Pro-STYLE.ttf"]]
    (when-let [url (io/resource name)]
      (Font/loadFont (str url) 12.0))))


;;; Creation of the stylesheets

;; Two separate registrations: Clojure maps can't have duplicate keys,
;; so each @font-face declaration needs its own css/register call.
(def ^:private font-c64-pro-mono
  (css/register
   ::font-c64-pro-mono
   {"@font-face" {:-font-family "'C64 Pro Mono'"
                  :-src         (str "url('" (io/resource "C64_Pro_Mono-STYLE.ttf") "')")}}))

(def ^:private font-c64-pro
  (css/register
   ::font-c64-pro
   {"@font-face" {:-font-family "'C64 Pro'"
                  :-src         (str "url('" (io/resource "C64_Pro-STYLE.ttf") "')")}}))

(def style
  (css/register
   ::c64
   {; Root
    ".root"
    {:-c64-bg                      "#6C5EB5"
     :-c64-border                  "#5047A0"
     :-c64-text                    "#8888FF"
     :-c64-text-dim                "#6060CC"
     :-c64-text-hi                 "#B8B8FF"
     :-c64-white                   "#FFFFFF"
     :-c64-black                   "#000000"
     :-c64-green                   "#55CC55"
     :-c64-lime                    "#00FF00"
     :-c64-red                     "#CC4444"
     :-c64-yellow                  "#BBBB44"
     :-c64-cyan                    "#6ABACC"
     :-c64-orange                  "#BB6622"
     :-fx-base                     "#6C5EB5"
     :-fx-background               "#6C5EB5"
     :-fx-control-inner-background "#4D3F96"
     :-fx-text-base-color          "#8888FF"
     :-fx-font-family              "\"C64 Pro Mono\""
     :-fx-font-size                "13px"
     :-fx-accent                   "#8888FF"
     :-fx-focus-color              "#FFFFFF"
     :-fx-faint-focus-color        "#8888FF22"}

    ; Scene background
    ".c64-scene-root"
    {:-fx-background-color "#4D3F96"
     :-fx-padding          "8px"}

    ; Title bar
    ".c64-title-bar"
    {:-fx-background-color "#5047A0"
     :-fx-padding          "6px 12px"
     :-fx-border-color     "#8888FF"
     :-fx-border-width     "0 0 2px 0"}

    ".c64-title-text"
    {:-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "16px"
     :-fx-text-fill   "#FFFFFF"}

    ".c64-subtitle-text"
    {:-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "11px"
     :-fx-text-fill   "#8888FF"}

    ".c64-log-bar"
    {:-fx-border-color     "#8888FF"
     :-fx-border-width     "1px 0 0 0"}

    ; Status indicators
    ".c64-config-loaded"
    {:-fx-text-fill   "#00FF00"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "11px"}

    ".c64-config-confirmed"
    {:-fx-text-fill   "#BB6622"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "11px"}

    ".c64-config-written"
    {:-fx-text-fill   "#CC4444"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "11px"}

    ".c64-config-saved"
    {:-fx-text-fill   "#6ABACC"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "11px"}

    ".c64-config-exported"
    {:-fx-text-fill   "#6ABACC"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "11px"}

    ".c64-config-imported"
    {:-fx-text-fill   "#55CC55"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "11px"}

    ".c64-config-applied"
    {:-fx-text-fill   "#BBBB44"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "11px"}

    ".c64-status-connected"
    {:-fx-text-fill   "#55CC55"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "12px"}

    ".c64-status-disconnected"
    {:-fx-text-fill   "#CC4444"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "12px"}

    ; Warning banner
    ".c64-warning-banner"
    {:-fx-background-color "#883300"
     :-fx-border-color     "#BB6622"
     :-fx-border-width     "2px"
     :-fx-padding          "8px 12px"}

    ".c64-warning-text"
    {:-fx-text-fill   "#FFAA00"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "13px"}

    ".c64-error-text"
    {:-fx-text-fill   "#ff3c00"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "13px"}

    ; Section panels
    ".c64-section"
    {:-fx-background-color "#5047A0"
     :-fx-border-color     "#7070CC"
     :-fx-border-width     "1px"
     :-fx-padding          "10px"
     :-fx-spacing          "6px"}

    ".c64-section-header"
    {:-fx-text-fill   "#CCCCFF"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "14px"
     :-fx-padding     "0 0 4px 0"}

    ".c64-section-separator"
    {:-fx-background-color "#7070CC"}

    ; Labels
    ".c64-label"
    {:-fx-text-fill   "#8888FF"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "13px"}

    ".c64-label-bright"
    {:-fx-text-fill   "#B8B8FF"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "13px"}

    ".c64-label-dim"
    {:-fx-text-fill   "#8080CC"          #_"#6060CC"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "12px"}

    ".c64-label-value"
    {:-fx-text-fill   "#FFFFFF"
     :-fx-font-family "\"C64 Pro Mono\""
     :-fx-font-size   "13px"}

    ; Buttons
    ".c64-button"
    {:-fx-background-color  "#4D3F96"
     :-fx-border-color      "#8888FF"
     :-fx-border-width      "1px"
     :-fx-text-fill         "#8888FF"
     :-fx-font-family       "\"C64 Pro Mono\""
     :-fx-font-size         "13px"
     :-fx-padding           "4px 12px"
     :-fx-cursor            "hand"
     :-fx-background-radius "0"
     :-fx-border-radius     "0"
     ":hover"               {:-fx-background-color "#6C5EB5"
                             :-fx-text-fill        "#FFFFFF"
                             :-fx-border-color     "#FFFFFF"}
     ":pressed"             {:-fx-background-color "#8888FF"
                             :-fx-text-fill        "#000000"}
     ":disabled"            {:-fx-opacity "0.4"}}

    ".c64-button-primary"
    {:-fx-background-color  "#4D3F96"
     :-fx-border-color      "#CCCCFF"
     :-fx-border-width      "2px"
     :-fx-text-fill         "#CCCCFF"
     :-fx-font-family       "\"C64 Pro Mono\""
     :-fx-font-size         "13px"
     :-fx-padding           "4px 12px"
     :-fx-cursor            "hand"
     :-fx-background-radius "0"
     :-fx-border-radius     "0"
     ":hover"               {:-fx-background-color "#8888FF"
                             :-fx-text-fill        "#000000"
                             :-fx-border-color     "#8888FF"}}

    ".c64-button-danger"
    {:-fx-background-color  "#4D3F96"
     :-fx-border-color      "#CC4444"
     :-fx-border-width      "1px"
     :-fx-text-fill         "#CC4444"
     :-fx-font-family       "\"C64 Pro Mono\""
     :-fx-font-size         "13px"
     :-fx-padding           "4px 12px"
     :-fx-cursor            "hand"
     :-fx-background-radius "0"
     :-fx-border-radius     "0"
     ":hover"               {:-fx-background-color "#CC4444"
                             :-fx-text-fill        "#FFFFFF"}}

    ; ToggleButton
    ".c64-toggle"
    {:-fx-background-color  "#3A2E80"
     :-fx-border-color      "#6060CC"
     :-fx-border-width      "1px"
     :-fx-text-fill         "#6060CC"
     :-fx-font-family       "\"C64 Pro Mono\""
     :-fx-font-size         "12px"
     :-fx-padding           "2px 8px"
     :-fx-background-radius "0"
     :-fx-border-radius     "0"
     :-fx-cursor            "hand"
     :-fx-min-width         "52px"
     ":selected"            {:-fx-background-color "#2A6C2A"
                             :-fx-border-color     "#55CC55"
                             :-fx-text-fill        "#55CC55"
                             ":hover"              {:-fx-border-color "#88FF88"
                                                    :-fx-text-fill    "#88FF88"}}
     ":hover"               {:-fx-border-color "#8888FF"
                             :-fx-text-fill    "#8888FF"}
     ":disabled"            {:-fx-opacity "0.4"}}

    ; ComboBox
    ".c64-combo-box"
    {:-fx-background-color  "#3A2E80"
     :-fx-border-color      "#6060CC"
     :-fx-border-width      "1px"
     :-fx-font-family       "\"C64 Pro Mono\""
     :-fx-font-size         "13px"
     :-fx-background-radius "0"
     :-fx-border-radius     "0"
     :-fx-pref-width        "210px"
     :-fx-min-width         "150px"
     " .list-cell"          {:-fx-background-color "transparent"
                             :-fx-text-fill        "#8888FF"
                             :-fx-font-family      "\"C64 Pro Mono\""
                             :-fx-font-size        "13px"}
     " .arrow-button"       {:-fx-background-color "#4D3F96"
                             :-fx-border-color     "#6060CC"}
     " .arrow"              {:-fx-background-color "#8888FF"
                             :-fx-padding          "3px 4px 3px 4px"
                             :-fx-shape            "\"M 0 0 h 7 l -3.5 4 z\""}
     ":showing"             {" .arrow-button" {:-fx-background-color "#6C5EB5"}}}

    ".c64-combo-box-wide"
    {:-fx-background-color  "#3A2E80"
     :-fx-border-color      "#6060CC"
     :-fx-border-width      "1px"
     :-fx-font-family       "\"C64 Pro Mono\""
     :-fx-font-size         "13px"
     :-fx-background-radius "0"
     :-fx-border-radius     "0"
     :-fx-pref-width        "300px"
     :-fx-min-width         "200px"
     " .list-cell"          {:-fx-background-color "transparent"
                             :-fx-text-fill        "#8888FF"
                             :-fx-font-family      "\"C64 Pro Mono\""
                             :-fx-font-size        "13px"}
     " .arrow-button"       {:-fx-background-color "#4D3F96"
                             :-fx-border-color     "#6060CC"}
     " .arrow"              {:-fx-background-color "#8888FF"
                             :-fx-padding          "3px 4px 3px 4px"
                             :-fx-shape            "\"M 0 0 h 7 l -3.5 4 z\""}}

    ;; Popup lives in its own PopupControl scene, must be top-level selector.
    ;; .combo-box-popup is the style class JavaFX always applies to the popup root.
    ".combo-box-popup"
    {" .list-view"            {:-fx-background-color  "#3A2E80"
                               :-fx-border-color      "#8888FF"
                               :-fx-border-width      "1px"
                               :-fx-background-radius "0"
                               :-fx-border-radius     "0"}
     " .list-view .list-cell" {:-fx-background-color "transparent"
                               :-fx-text-fill        "#8888FF"
                               :-fx-font-family      "\"C64 Pro Mono\""
                               :-fx-font-size        "13px"
                               :-fx-padding          "4px 8px"
                               ":hover"              {:-fx-background-color "#4D3F96"
                                                      :-fx-text-fill        "#FFFFFF"}
                               ":selected"           {:-fx-background-color "#5047A0"
                                                      :-fx-text-fill        "#FFFFFF"}
                               ":filled:hover"       {:-fx-background-color "#4D3F96"
                                                      :-fx-text-fill        "#FFFFFF"}
                               ":filled:selected"    {:-fx-background-color "#5047A0"
                                                      :-fx-text-fill        "#FFFFFF"}}}


    ; Slider
    ".c64-slider"
    {" .track"  {:-fx-background-color  "#3A2E80"
                 :-fx-border-color      "#6060CC"
                 :-fx-border-width      "1px"
                 :-fx-background-radius "0"
                 :-fx-border-radius     "0"}
     " .thumb"  {:-fx-background-color  "#8888FF"
                 :-fx-background-radius "0"
                 :-fx-border-radius     "0"
                 :-fx-effect            "none"
                 :-fx-cursor            "hand"
                 ":hover"               {:-fx-background-color "#FFFFFF"}}
     ":focused" {" .thumb" {:-fx-background-color "#CCCCFF"}}}

    ; TextField
    ".c64-text-field"
    {:-fx-background-color    "#3A2E80"
     :-fx-border-color        "#6060CC"
     :-fx-border-width        "1px"
     :-fx-text-fill           "#8888FF"
     :-fx-font-family         "\"C64 Pro Mono\""
     :-fx-font-size           "13px"
     :-fx-background-radius   "0"
     :-fx-border-radius       "0"
     :-fx-padding             "3px 6px"
     :-fx-highlight-fill      "#8888FF"
     :-fx-highlight-text-fill "#000000"
     ":focused"               {:-fx-border-color "#FFFFFF"}}

    ; Log / terminal area
    ".c64-log-area"
    {:-fx-control-inner-background "#1A1060"
     :-fx-background-color         "#1A1060"
     :-fx-border-color             "#5047A0"
     :-fx-border-width             "1px"
     :-fx-font-family              "\"C64 Pro Mono\""
     :-fx-font-size                "12px"
     :-fx-text-fill                "#55CC55"
     :-fx-highlight-fill           "#55CC55"
     :-fx-highlight-text-fill      "#000000"
     :-fx-background-radius        "0"
     :-fx-border-radius            "0"
     " .content"                   {:-fx-background-color "#1A1060"}
     " .scroll-pane"               {:-fx-background-color "#1A1060"}}

    ; ScrollPane
    ".c64-scroll-pane"
    {:-fx-background-color            "transparent"
     :-fx-border-color                "transparent"
     :-fx-padding                     "0"
     " > .viewport"                   {:-fx-background-color "transparent"}
     " .scroll-bar:vertical"          {:-fx-background-color "#3A2E80"
                                       :-fx-border-color     "#5047A0"}
     " .scroll-bar:horizontal"        {:-fx-background-color "#3A2E80"
                                       :-fx-border-color     "#5047A0"}
     " .scroll-bar .thumb"            {:-fx-background-color  "#6060CC"
                                       :-fx-background-radius "0"}
     " .scroll-bar .increment-button" {:-fx-background-color "#4D3F96"}
     " .scroll-bar .decrement-button" {:-fx-background-color "#4D3F96"}}

    ; Separator
    ".c64-separator"
    {" .line" {:-fx-border-color "#5047A0"
               :-fx-border-width "1px"}}

    ; Tab pane
    ".tab-pane"
    {:-fx-tab-min-height                        "28px"
     " .tab-header-area .tab-header-background" {:-fx-background-color "#3A2E80"}
     " .tab"
     {:-fx-background-color  "#3A2E80"
      :-fx-border-color      "#5047A0"
      :-fx-border-width      "1px 1px 0 1px"
      :-fx-background-radius "0"
      :-fx-border-radius     "0"
      " .tab-label"          {:-fx-text-fill   "#6060CC"
                              :-fx-font-family "\"C64 Pro Mono\""
                              :-fx-font-size   "13px"}
      ":selected"            {:-fx-background-color "#5047A0"
                              " .tab-label"         {:-fx-text-fill "#FFFFFF"}}
      ":hover"               {" .tab-label" {:-fx-text-fill "#8888FF"}}}
     " .tab-content-area"                       {:-fx-background-color "#4D3F96"
                                                 :-fx-border-color     "#5047A0"
                                                 :-fx-border-width     "1px"}}

    ; Left nav list
    ".c64-nav"
    {:-fx-background-color "#3A2E80"
     :-fx-border-color     "#5047A0"
     :-fx-border-width     "0 1px 0 0"
     :-fx-padding          "4px 0"
     :-fx-min-width        "150px"
     :-fx-pref-width       "150px"}

    ".c64-nav-item"
    {:-fx-background-color  "transparent"
     :-fx-text-fill         "#8888FF"
     :-fx-font-family       "\"C64 Pro Mono\""
     :-fx-font-size         "13px"
     :-fx-padding           "6px 12px"
     :-fx-alignment         "center-left"
     :-fx-border-color      "transparent"
     :-fx-background-radius "0"
     :-fx-border-radius     "0"
     :-fx-cursor            "hand"
     :-fx-min-width         "148px"
     :-fx-pref-width        "148px"
     ":hover"               {:-fx-background-color "#4D3F96"
                             :-fx-text-fill        "#CCCCFF"}}

    ".c64-nav-item-active"
    {:-fx-background-color "#5047A0"
     :-fx-text-fill        "#FFFFFF"
     :-fx-border-color     "transparent #8888FF transparent transparent"
     :-fx-border-width     "0 2px 0 0"}

    ; Grid / layout helpers
    ".c64-grid"
    {:-fx-hgap    "12px"
     :-fx-vgap    "6px"
     :-fx-padding "8px"}

    ".c64-hbox"
    {:-fx-spacing   "8px"
     :-fx-alignment "center-left"}

    ".c64-vbox"
    {:-fx-spacing "6px"}

    ; Chip type indicators
    ".c64-chip-real"    {:-fx-text-fill "#55CC55"}
    ".c64-chip-unknown" {:-fx-text-fill "#8888FF"}
    ".c64-chip-clone"   {:-fx-text-fill "#6ABACC"}

    ; Cursor
    ".c64-cursor"
    {:-fx-text-fill "#FFFFFF"}

    ; Text
    ".c64-text-middle"
    {:-fx-alignment "center"}

    ".c64-text-wrap"
    {:-fx-wrap-text true}}))


;;; Register the stylesheets

(def stylesheets
  [(:cljfx.css/url font-c64-pro-mono)
   (:cljfx.css/url font-c64-pro)
   (:cljfx.css/url style)])
