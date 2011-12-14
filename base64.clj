(ns base64)
(def base64-char-table (char-array 64))
(doseq [i (range 26)] (aset base64-char-table i (char (+ 65 i))))
(doseq [i (range 26)] (aset base64-char-table (+ i 26) (char (+ 97 i))))
(doseq [i (range 10)] (aset base64-char-table (+ i 52) (char (+ 48 i))))
(aset base64-char-table 62 \+)
(aset base64-char-table 63 \/)
;TODO - figure out how to make the reader compile this.

(defn get-first-encoded-char-index [c]
  (bit-shift-right (int c) 2))

(defn get-second-encoded-char-index [c1  c2]
  (bit-or (bit-shift-right (int c2) 4) (bit-shift-left (bit-and (int c1) 3) 4)))
;third char

(defn get-third-encoded-char-index [c2  c3]
  (bit-or (bit-shift-left (bit-and (int c2) 15) 2) (bit-shift-right (int c3) 6)))
;magic numbers come from the likes of (Long/valueOf "00111111" 2) => 63
;4th char
(defn get-fourth-encoded-char-index [c3]
  (bit-and (int c3) 63))

(defn get-first-encoded-char [c1]
  (aget base64-char-table (get-first-encoded-char-index c1)))

(defn get-second-encoded-char [c1  c2]
  (aget base64-char-table (get-second-encoded-char-index c1 c2)))

(defn get-third-encoded-char [c2  c3]
  (aget base64-char-table (get-third-encoded-char-index c2 c3)))

(defn get-fourth-encoded-char [c3]
  (aget base64-char-table (get-fourth-encoded-char-index c3)))

(defn encode-chars 
  ([c1 c2 c3]
   (let [e1 (get-first-encoded-char c1)
         e2 (get-second-encoded-char c1 c2)
         e3 (get-third-encoded-char c2 c3)
         e4 (get-fourth-encoded-char c3)]
     (str e1 e2 e3 e4)))
  ([c1 c2]
   (let [e1 (get-first-encoded-char c1)
         e2 (get-second-encoded-char c1 c2)
         e3 (get-third-encoded-char c2 0)
         e4 \=]
     (str e1 e2 e3 e4)))
  ([c1]
   (let [e1 (get-first-encoded-char c1)
         e2 (get-second-encoded-char c1 0)
         e3 \=
         e4 \=]
     (str e1 e2 e3 e4))))

(defn encode-char-array
  [[c1 c2 c3]]
  (cond c3
        (encode-chars c1 c2 c3)
        c2
        (encode-chars c1 c2)
        c1
        (encode-chars c1)))

(defn encode
  "Encodes the given string in base64, according to the rules for the last two characters and
  padding that the SMTP spec requires."
  [s]
  (str 
    (apply str (map encode-char-array (partition 3 s)))
    (encode-char-array (take-last (mod (count s) 3) s))))
