(ns usbsid.sid.chip
  (:require
   [usbsid.driver :as driver]))

(def data-ids
  {:READ_CLONECHIP    0xa0
   :WRITE_CLONECHIP   0xb0
   :VERIFICATION_BYTE 0x7f
   :END_BYTE          0x8f
   :TERMINATION_BYTE  0xff})

(def read-ids
  {:FPGASID 0xa5})

(defn read-clone-chip
  [idkey addr]
  (driver/read-config-command
   (:READ_CLONECHIP data-ids)
   {:a (idkey read-ids)
    :b addr}))
