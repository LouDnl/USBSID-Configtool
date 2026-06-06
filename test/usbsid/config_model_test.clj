(ns usbsid.config-model-test
  (:require
   [clojure.test         :refer [deftest is testing are]]
   [usbsid.config-model  :as model]))

(deftest clock-rate-lookup
  (testing "clock-rate-by-id maps id → key"
    (are [id k] (= k (:key (get model/clock-rate-by-id id)))
      0 :default
      1 :pal
      2 :ntsc
      3 :drean
      4 :ntsc2))
  (testing "clock-rate-by-key maps key → id"
    (are [k id] (= id (:id (get model/clock-rate-by-key k)))
      :default 0
      :pal     1
      :ntsc    2
      :drean   3
      :ntsc2   4)))

(deftest chip-type-lookup
  (testing "chip-type-by-id"
    (are [id k] (= k (:key (get model/chip-type-by-id id)))
      0 :real
      1 :unknown
      2 :skpico
      3 :armsid
      4 :arm2sid
      5 :fpgasid
      9 :sidemu))
  (testing "chip-type-by-key"
    (are [k id] (= id (:id (get model/chip-type-by-key k)))
      :real    0
      :unknown 1
      :skpico  2
      :armsid  3)))

(deftest sid-type-lookup
  (testing "sid-type-by-id"
    (are [id k] (= k (:key (get model/sid-type-by-id id)))
      0 :unknown
      1 :na
      2 :mos8580
      3 :mos6581
      4 :fmopl))
  (testing "sid-type-by-key"
    (are [k id] (= id (:id (get model/sid-type-by-key k)))
      :unknown 0
      :na      1
      :mos8580 2
      :mos6581 3
      :fmopl   4)))

(deftest preset-lookup
  (testing "14 presets defined"
    (is (= 14 (count model/presets))))
  (testing "preset-by-id"
    (is (= :single-s1  (:key (get model/preset-by-id 0))))
    (is (= :dual-both  (:key (get model/preset-by-id 2))))
    (is (= :quad       (:key (get model/preset-by-id 7))))
    (is (= :quad-flipmix (:key (get model/preset-by-id 13)))))
  (testing "preset-by-key"
    (is (= 0  (:id (get model/preset-by-key :single-s1))))
    (is (= 7  (:id (get model/preset-by-key :quad))))
    (is (= 13 (:id (get model/preset-by-key :quad-flipmix))))))

(deftest initial-config-valid
  (testing "initial-config has required keys"
    (let [c model/initial-config]
      (is (some? (:clock-rate c)))
      (is (some? (:socket-one c)))
      (is (some? (:socket-two c)))
      (is (some? (:led c)))
      (is (some? (:rgbled c)))))
  (testing "initial-config clock-rate is valid key"
    (is (some? (get model/clock-rate-by-key (:clock-rate model/initial-config)))))
  (testing "initial-config socket chiptyp is valid"
    (is (some? (get model/chip-type-by-key (get-in model/initial-config [:socket-one :chiptype]))))))
