(ns com.mtzcg.history.dates-test
  (:require [clojure.test :refer [deftest is]]
            [com.mtzcg.history.lib.dates :as dates]))

(deftest parse-partial-date
  (is (= [19520101 19521231] (dates/parse-partial-date "1952")))
  (is (= [19520201 19520229] (dates/parse-partial-date "1952-02")))
  (is (= [19520614 19520614] (dates/parse-partial-date " 1952-06-14 ")))
  (is (nil? (dates/parse-partial-date "circa 1950")))
  (is (nil? (dates/parse-partial-date "1952-13")))
  (is (nil? (dates/parse-partial-date nil))))
