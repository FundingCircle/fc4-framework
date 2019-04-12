(ns fc4.cli.format
  "CLI subcommand that invokes fc4.io.yaml/process-diagram-file on each
  Structurizr Express YAML file specified via command-line args."
  (:require [fc4.cli.util :as cu :refer [debug fail]]
            [fc4.io.yaml :refer [process-diagram-file]]
            [fc4.io.util :refer [print-now]]))

(defn -main
  ;; NB: if and when we add options we’ll probably want to use
  ;; tools.cli/parse-opts to parse them.
  ;;
  ;; TODO: Actually, now that I think about it, we should probably add a --help
  ;; option ASAP.
  ;;
  ;; TODO: add a command-line flag that sets cu/*debug* to true
  [& paths]
  (try
    (doseq [path paths]
      (print-now path ": formatting...")
      (process-diagram-file path)
      (println "✅"))
    (catch Exception e
      ; TODO: maybe use cu/debug print out stack trace and ex-data if present?
      (fail (.getMessage e)))))
