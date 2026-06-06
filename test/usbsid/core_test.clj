(ns usbsid.core-test
  (:require
   [clojure.test          :refer [deftest is testing]]
   [usbsid.config-model   :as model]
   [usbsid.state          :as state]))

(deftest namespace-loads
  (testing "config-model namespace loads"
    (is (seq model/clock-rates))
    (is (seq model/chip-types))
    (is (seq model/presets)))
  (testing "state atom initialises"
    (is (map? @state/*state))
    (is (= :disconnected (get-in @state/*state [:connection :status])))))
