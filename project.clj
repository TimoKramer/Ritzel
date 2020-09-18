(defproject ritzel "0.1.0-SNAPSHOT"
  :description "Pulumi REST service"
  :url "https://github.com/timokramer/ritzel"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [io.replikativ/datahike "0.3.2-SNAPSHOT"]
                 [buddy/buddy-auth "2.2.0"]
                 [ring/ring-core "1.8.1"]
                 [ring/ring-jetty-adapter "1.8.1"]
                 [metosin/reitit "0.5.5"]
                 [com.taoensso/timbre "4.10.0"]
                 [mount "0.1.16"]
                 [environ "1.2.0"]]
  :profiles {:dev {:dependencies [[clj-http "3.10.1"]]}}
  :source-paths ["src"]
  :main ritzel.core
  :aot [ritzel.core]
  :repl-options {:init-ns ritzel.core}
  :uberjar-name "ritzel-standalone.jar"
  :plugins [[lein-cljfmt "0.6.8"]])
