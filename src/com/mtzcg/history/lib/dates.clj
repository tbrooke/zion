(ns com.mtzcg.history.lib.dates
  (:import (java.time LocalDate YearMonth)))

(defn- ymd [^LocalDate d]
  (+ (* 10000 (.getYear d)) (* 100 (.getMonthValue d)) (.getDayOfMonth d)))

(defn parse-partial-date
  "Parses \"1952\", \"1952-06\" or \"1952-06-14\" into [start end] YYYYMMDD ints
  covering the whole period. Returns nil for anything else."
  [s]
  (when-some [[_ y m d] (some->> s (re-matches #"\s*(\d{4})(?:-(\d{1,2})(?:-(\d{1,2}))?)?\s*"))]
    (try
      (let [y (parse-long y)]
        (cond
          d (let [date (LocalDate/of y (parse-long m) (parse-long d))]
              [(ymd date) (ymd date)])
          m (let [ym (YearMonth/of y (parse-long m))]
              [(ymd (.atDay ym 1)) (ymd (.atEndOfMonth ym))])
          :else [(+ (* 10000 y) 101) (+ (* 10000 y) 1231)]))
      (catch java.time.DateTimeException _ nil))))
