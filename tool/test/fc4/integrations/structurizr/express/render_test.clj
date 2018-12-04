(ns fc4.integrations.structurizr.express.render-test
  (:require [fc4.integrations.structurizr.express.render :as r]
            [clojure.java.io                             :as io :refer [file input-stream]]
            [image-resizer.core                                 :refer [resize]])
(defn binary-slurp
  "Based on https://stackoverflow.com/a/29640320/7012"
  [file-or-file-path]
  (let [file (file file-or-file-path) ; no-op if the value is already a File
        result (byte-array (.length file))]
    (with-open [in (DataInputStream. (input-stream file))]
      (.readFully in result))
    result))

(defn binary-spit [f data]
  (with-open [out (io/output-stream (file f))]
    (.write out data)))

(defn temp-png-file [basename] (java.io.File/createTempFile basename ".png"))

(def max-allowable-image-difference
  ;; This threshold might seem low, but the diffing algorithm is
  ;; giving very low results for some reason. This threshold seems
  ;; to be sufficient to make the random watermark effectively ignored
  ;; while other, more significant changes (to my eye) seem to be
  ;; caught. Still, this is pretty unscientific, so it might be worth
  ;; looking into making this more precise and methodical.
  0.005)

(deftest render
  (testing "happy paths"
    (testing "rendering a Structurizr Express file"
      (let [yaml (slurp (str dir "diagram_valid_cleaned.yaml"))
            {:keys [::r/png-bytes ::r/stderr] :as result} (r/render yaml)
            actual-bytes png-bytes
            expected-bytes (binary-slurp (str dir "diagram_valid_cleaned_expected.png"))
            difference (->> [actual-bytes expected-bytes]
                            (map bytes->buffered-image)
                            (map #(resize % 1000 1000))
                            (reduce image-diff))]
        (is (s/valid? ::r/result result) (s/explain-str ::r/result result))
        (is (<= difference max-allowable-image-difference)
            ;; NB: below in addition to returning a message we write the actual
            ;; bytes out to the file system, to help with debugging. But
            ;; apparently `is` evaluates this `msg` arg eagerly, so it’s
            ;; evaluated even if the assertion is true. This means that even
            ;; when the test passes the “expected” file is written out to the
            ;; filesystem. So TODO: maybe we should do something about this.
            (let [expected-debug-fp (temp-png-file "rendered_expected.png")
                  actual-debug-fp (temp-png-file "rendered_actual.png")]
              (binary-spit expected-debug-fp expected-bytes)
              (binary-spit actual-debug-fp actual-bytes)
              (str stderr
                   "Images are "
                   difference
                   " different, which is higher than the threshold of "
                   max-allowable-image-difference
                   "\n“expected” PNG written to:" (.getPath expected-debug-fp)
                   "\n“actual” PNG written to:" (.getPath actual-debug-fp)))))))
  (testing "sad paths"
    (testing "various invalid inputs"
      (let [result (r/render "this is not YAML? Or I guess maybe it is?")]
        (is

         result)))))
