(ns usbsid.state-test
  "Crash test dummies"
  (:require
   [clojure.test  :refer [deftest is testing use-fixtures]]
   [usbsid.state  :as state]))

(defn reset-state-fixture [f]
  (reset! state/*state state/initial-state)
  (f)
  (reset! state/*state state/initial-state))

(use-fixtures :each reset-state-fixture)

(deftest initial-state-structure
  (testing "initial state has required keys"
    (let [s @state/*state]
      (is (= :disconnected (get-in s [:connection :status])))
      (is (nil? (get-in s [:connection :fw-version])))
      (is (nil? (get-in s [:connection :pcb-version])))
      (is (some? (:config s)))
      (is (vector? (:log s)))
      (is (= :about (:active-section s)))
      (is (false? (:dirty s))))))

(deftest log-appends
  (testing "log! appends message"
    (state/log! "hello")
    (let [log (:log @state/*state)]
      (is (> (count log) 1))
      (is (= "hello" (last log)))))
  (testing "multiple log! calls accumulate"
    (state/log! "first")
    (state/log! "second")
    (let [log (:log @state/*state)]
      (is (some #(= "first" %) log))
      (is (some #(= "second" %) log)))))

(deftest set-section
  (testing "set-section! changes active-section"
    (state/set-section! :clock)
    (is (= :clock (:active-section @state/*state)))
    (state/set-section! :leds)
    (is (= :leds (:active-section @state/*state)))))

(deftest set-config-value
  (testing "set-config-value! updates nested path"
    (state/set-config-value! [:led :enabled] false)
    (is (false? (get-in @state/*state [:config :led :enabled]))))
  (testing "set-config-value! marks dirty"
    (state/set-config-value! [:clock-rate] :ntsc)
    (is (true? (:dirty @state/*state))))
  (testing "set-config-value! updates socket one chiptype"
    (state/set-config-value! [:socket-one :chiptype] :armsid)
    (is (= :armsid (get-in @state/*state [:config :socket-one :chiptype]))))
  (testing "set-config-value! deep path"
    (state/set-config-value! [:rgbled :brightness] 200)
    (is (= 200 (get-in @state/*state [:config :rgbled :brightness])))))

(deftest set-connection
  (testing "set-connection! :connected"
    (state/set-connection! :connected "v0.6.0" "1.5")
    (let [conn (:connection @state/*state)]
      (is (= :connected (:status conn)))
      (is (= "v0.6.0"   (:fw-version conn)))
      (is (= "1.5"      (:pcb-version conn)))))
  (testing "set-connection! :disconnected clears versions"
    (state/set-connection! :connected "v0.6.0" "1.5")
    (state/set-connection! :disconnected nil nil)
    (let [conn (:connection @state/*state)]
      (is (= :disconnected (:status conn)))
      (is (nil? (:fw-version conn))))))

(deftest config-value-types
  (testing "boolean set-config-value! stores booleans not ints"
    (state/set-config-value! [:led :enabled] true)
    (is (true? (get-in @state/*state [:config :led :enabled])))
    (state/set-config-value! [:led :enabled] false)
    (is (false? (get-in @state/*state [:config :led :enabled]))))
  (testing "integer set-config-value!"
    (state/set-config-value! [:rgbled :brightness] 127)
    (is (= 127 (get-in @state/*state [:config :rgbled :brightness])))))
