(ns usbsid.ini-io
  "Read and write the damn thing"
  (:require
   [clojure.string :as str]
   [usbsid.config-model :as model]))


;;; INI label tables (match cfg_usbsid.c)

(def ^:private chiptype-labels
  ["MOS" "Unknown" "SKPico" "ARMSID" "ARM2SID" "FPGASID" "RedipSID" "PDSID" "BackSID" "SIDEmu"])

(def ^:private sidtype-labels
  ["Unknown" "N/A" "8580" "6581" "FMOpl"])


;;; Config map -> INI string (export)

(defn- bool->s
  [v] (if v "True" "False"))

(defn- dual->s
  [v] (if v "Enabled" "Disabled"))

(defn- audio->s
  [v] (if v "Stereo" "Mono"))

(defn- chip->s
  [k]
  (let [id (get-in model/chip-type-by-key [k :id] 1)]
    (get chiptype-labels id "Unknown")))

(defn- sid->s
  [k]
  (let [id (get-in model/sid-type-by-key [k :id] 0)]
    (get sidtype-labels id "Unknown")))

(defn- clk->int
  [k]
  (get-in model/clock-rate-by-key [k :value] 985248))

(defn config->ini
  "Serialises config map to INI string compatible with cfg_usbsid.c format."
  [cfg fw-version]
  (let [s1  (:socket-one cfg)
        s2  (:socket-two cfg)
        led (:led cfg)
        rgb (:rgbled cfg)]
    (str/join
     "\n"
     ["[General]"
      "; Version is for reference only"
      (str "version = " (or fw-version "v0.0.0-DEFAULT.19000101"))
      "; Possible clockrates: 1000000, 985248, 1022727, 1023440, 1022730"
      (str "clock_rate = " (clk->int (:clock-rate cfg)))
      "; Possible options: False, True"
      (str "lock_clockrate = " (bool->s (:lock-clockrate cfg)))
      "; Possible options: False, True"
      (str "external_clock = " (bool->s (:external-clock cfg)))
      ""
      "[socketOne]"
      "; Possible options: False, True"
      (str "enabled = " (bool->s (:enabled s1)))
      "; Possible options: Disabled, Enabled"
      (str "dualsid = " (dual->s (:dualsid s1)))
      (str "; Possible chiptypes: " (str/join ", " chiptype-labels))
      (str "chiptype = " (chip->s (:chiptype s1)))
      (str "; Possible sidtypes: " (str/join ", " sidtype-labels))
      (str "sid1type = " (sid->s (get-in s1 [:sid1 :type])))
      (str "sid2type = " (sid->s (get-in s1 [:sid2 :type])))
      ""
      "[socketTwo]"
      "; Possible options: False, True"
      (str "enabled = " (bool->s (:enabled s2)))
      "; Possible options: Disabled, Enabled"
      (str "dualsid = " (dual->s (:dualsid s2)))
      (str "; Possible chiptypes: " (str/join ", " chiptype-labels))
      (str "chiptype = " (chip->s (:chiptype s2)))
      (str "; Possible sidtypes: " (str/join ", " sidtype-labels))
      (str "sid1type = " (sid->s (get-in s2 [:sid1 :type])))
      (str "sid2type = " (sid->s (get-in s2 [:sid2 :type])))
      ""
      "[Features]"
      "; Possible options: False, True"
      (str "mirrored = " (bool->s (:mirrored cfg)))
      (str "flipped  = " (bool->s (:flipped cfg)))
      (str "mixed    = " (bool->s (:mixed cfg)))
      ""
      "[LED]"
      "; Possible options: False, True"
      (str "enabled = " (bool->s (:enabled led)))
      "; Possible options: False, True"
      (str "idle_breathe = " (bool->s (:idle-breathe led)))
      ""
      "[RGBLED]"
      "; Possible options: False, True"
      (str "enabled = " (bool->s (:enabled rgb)))
      "; Possible options: False, True"
      (str "idle_breathe = " (bool->s (:idle-breathe rgb)))
      "; Possible brightness values 0 ~ 255"
      (str "brightness = " (:brightness rgb))
      "; Possible sids to use are 1, 2, 3 or 4"
      (str "sid_to_use = " (max 1 (:sid-to-use rgb)))
      ""
      "[FMOPL]"
      "; Possible options: False, True"
      (str "enabled = " (bool->s (get-in cfg [:fmopl :enabled])))
      ""
      "[Audioswitch]"
      "; Possible options: Mono, Stereo"
      (str "set_to = " (audio->s (:stereo-en cfg)))
      "; Possible options: False, True"
      (str "lock_audio_switch = " (bool->s (:lock-audio-sw cfg)))
      ""])))


;;; INI string -> config map (import)

(defn- s->bool
  [s]
  (= "True" (str/trim s)))

(defn- s->dual
  [s]
  (= "Enabled" (str/trim s)))

(defn- s->audio
  [s]
  (= "Stereo" (str/trim s)))

(defn- s->chip
  [s]
  (let [v (str/trim s)
        ; "Real" is an old format alias for "MOS"
        v (if (= v "Real") "MOS" v)
        ; "Clone" was used generically - map to :unknown
        v (if (= v "Clone") "Unknown" v)
        id (-> (.indexOf chiptype-labels v)
               (#(if (neg? %) 1 %)))]
    (get-in model/chip-type-by-id [id :key] :unknown)))

(defn- s->sid
  [s]
  (let [v (str/trim s)
        ; Normalise "MOS8580" -> "8580", "MOS6581" -> "6581" for compat
        v (cond (= v "MOS8580") "8580"
                (= v "MOS6581") "6581"
                :else v)
        id (.indexOf sidtype-labels v)]
    (:key (get model/sid-type-by-id (if (neg? id) 0 id) {:key :unknown}))))

(defn- s->clk
  [s]
  (let [v (try (Integer/parseInt (str/trim s)) (catch Exception _ 985248))]
    (:key (first (filter (comp #{v} :value) model/clock-rates)) :pal)))

(defn- parse-ini-lines
  [lines]
  (reduce
   (fn [{:keys [section] :as acc} line]
     (let [line (str/trim line)]
       (cond
         (str/starts-with? line ";")  acc ; comment
         (str/starts-with? line "#")  acc ; comment
         (str/blank? line)            acc
         (str/starts-with? line "[")
         (assoc acc :section (-> line
                                 (str/replace "[" "")
                                 (str/replace "]" "")
                                 str/trim))
         (str/includes? line "=")
         (let [[k v] (str/split line #"=" 2)]
           (assoc-in acc [:kv section (str/trim k)] (str/trim v)))
         :else acc)))
   {:section nil :kv {}}
   lines))

(defn ini->config
  "Parses INI string into partial config map.
   Merges over base-config to preserve fields not present in INI (e.g. FMOpl sidno)."
  [ini-str base-config]
  (let [{:keys [kv]} (parse-ini-lines (str/split-lines ini-str))
        g            (fn [section k] (get-in kv [section k]))
        upd          (fn [cfg path f section k]
                       (if-let [v (g section k)]
                         (assoc-in cfg path (f v))
                         cfg))]
    (-> base-config
        (upd [:clock-rate]             s->clk  "General"   "clock_rate")
        (upd [:lock-clockrate]         s->bool "General"   "lock_clockrate")
        (upd [:external-clock]         s->bool "General"   "external_clock")
        (upd [:socket-one :enabled]    s->bool "socketOne" "enabled")
        (upd [:socket-one :dualsid]    s->dual "socketOne" "dualsid")
        (upd [:socket-one :chiptype]   s->chip "socketOne" "chiptype")
        (upd [:socket-one :sid1 :type] s->sid  "socketOne" "sid1type")
        (upd [:socket-one :sid2 :type] s->sid  "socketOne" "sid2type")
        (upd [:socket-two :enabled]    s->bool "socketTwo" "enabled")
        (upd [:socket-two :dualsid]    s->dual "socketTwo" "dualsid")
        (upd [:socket-two :chiptype]   s->chip "socketTwo" "chiptype")
        (upd [:socket-two :sid1 :type] s->sid  "socketTwo" "sid1type")
        (upd [:socket-two :sid2 :type] s->sid  "socketTwo" "sid2type")
        (upd [:mirrored]               s->bool "Features"  "mirrored")
        (upd [:mirrored]               s->bool "socketTwo" "act_as_one") ; compat
        (upd [:flipped]                s->bool "Features"  "flipped")
        (upd [:mixed]                  s->bool "Features"  "mixed")
        (upd [:led :enabled]           s->bool "LED"       "enabled")
        (upd [:led :idle-breathe]      s->bool "LED"       "idle_breathe")
        (upd [:rgbled :enabled]        s->bool "RGBLED"    "enabled")
        (upd [:rgbled :idle-breathe]   s->bool "RGBLED"    "idle_breathe")
        (upd [:rgbled :brightness]
             (fn [s] (-> s Integer/parseInt (max 0) (min 255)))
             "RGBLED" "brightness")
        (upd [:rgbled :sid-to-use]
             (fn [s] (let [v (Integer/parseInt s)]
                       (if (<= 1 v 4) v -1)))
             "RGBLED" "sid_to_use")
        (upd [:fmopl :enabled]         s->bool  "FMOPL"       "enabled")
        (upd [:stereo-en]              s->audio "Audioswitch" "set_to")
        (upd [:lock-audio-sw]          s->bool  "Audioswitch" "lock_audio_switch"))))
