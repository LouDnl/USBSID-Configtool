(ns usbsid.ui.sections.welcome
  "So, what's it all about then?!"
  (:require
   [usbsid.config-model :refer [parse-fw-version]]
   [usbsid.ui.sections.common :as common]
   [usbsid.ui.widgets :as w]))

(defn about-section
  "Generates the about tab, pagina uno"
  [{:keys [connection]}]
  (let [fw       (get-in connection [:fw-version])
        pcb      (get-in connection [:pcb-version])
        fwverint (parse-fw-version fw)
        warningsection (if (>= fwverint 70)
                         common/warning-text-block
                         common/warning-text-block-legacy)]
    {:fx/type     :v-box
     :style-class "c64-vbox"
     :spacing     10
     :padding     {:top    8
                   :right  8
                   :bottom 8
                   :left   8}
     :children
     [common/welcome-text-block

      (w/c64-separator)
      common/info-text-block

      (w/c64-separator)
      warningsection

      (w/c64-separator)
      common/about-text-block

      (w/c64-separator)
      (common/deviceinfo-text-block fw pcb)

      (w/c64-separator)
      common/credits-text-block

      (w/c64-separator)
      common/revisions-text-block

      (w/c64-separator)
      common/links-text-block]}))
