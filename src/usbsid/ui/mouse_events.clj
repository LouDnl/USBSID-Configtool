(ns usbsid.ui.mouse-events
  (:require
   [cljfx.api :as fx]
   [cljfx.lifecycle :as lifecycle]
   [cljfx.mutator :as mutator])
  (:import
   [javafx.scene Node]
   [javafx.stage Popup]))

(def popup-width 320)

;; Anchors a Popup to a Node by horizontally centering it under the node's
;; top-left corner mapped to screen coords.
(def prop-shown-on
  (fx/make-prop
   (mutator/adder-remover
    (fn [^Popup popup ^Node node]
      (let [bounds   (.getBoundsInLocal node)
            ;; Anchor popup's bottom-left 4px above the button's top edge —
            ;; combined with :anchor-location :window-bottom-left the popup
            ;; grows upward and never overlaps the trigger (overlap would
            ;; fire MOUSE_EXITED and cause a cursor-flicker loop).
            node-pos (.localToScreen node
                                     (* 0.5 (.getWidth bounds))
                                     -4.0)]
        (.show popup node
               (- (.getX node-pos) (* 0.5 popup-width))
               (.getY node-pos))))
    (fn [^Popup popup _]
      (.hide popup)))
   lifecycle/dynamic))

(defn popup
  "Wrap a button so it triggers a hover popup. Args:
     :hover-popup  - current popup key from app state (nil = none shown)
     :popup-key    - this button's id; popup shown when (= hover-popup popup-key)
     :popup-text   - text shown in the popup body
     :button       - button prop map without :fx/type (text/style-class/on-action/etc.)"
  [{:keys [hover-popup popup-key popup-text button]}]
  (let [show?   (= hover-popup popup-key)
        ;; Preserve caller's :fx/type (toggle-button has :selected,
        ;; plain :button doesn't). Default to :button when caller omits.
        trigger (-> button
                    (update :fx/type #(or % :button))
                    (assoc  :on-mouse-entered {:event/type :popup-show :key popup-key}
                            :on-mouse-exited  {:event/type :popup-hide :key popup-key}))]
    {:fx/type fx/ext-let-refs
     :refs    {::trigger trigger}
     :desc    {:fx/type fx/ext-let-refs
               :refs    (cond-> {}
                          show?
                          (assoc ::popup
                                 {:fx/type         :popup
                                  :anchor-location :window-bottom-left
                                  :auto-hide       false
                                  :auto-fix        true
                                  :content         [{:fx/type     :label
                                                     :style-class "c64-popup"
                                                     :min-width   popup-width
                                                     :max-width   popup-width
                                                     :pref-width  popup-width
                                                     :wrap-text   true
                                                     :effect      {:fx/type :drop-shadow}
                                                     :text        popup-text}]
                                  prop-shown-on    {:fx/type fx/ext-get-ref :ref ::trigger}}))
               :desc    {:fx/type fx/ext-get-ref :ref ::trigger}}}))
