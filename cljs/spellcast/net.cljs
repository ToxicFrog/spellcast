(ns spellcast.net
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! go]]
            [reagent.core :as r]
  ))

(defn poll
  "Long-poll the given view path and whenever it changes, reset! the result into an atom. If called without a stamp, uses a stamp of 0; subsequent refreshes get the stamp from the X-Stamp header.
  Delays 100ms between polls; on error, backs off to 5s and sets the atom to nil.
  Returns the atom."
  ([path] (poll path 0))
  ([path stamp] (poll path stamp (r/atom nil)))
  ([path stamp atom]
    (go
      (let [url (str path "/" stamp)
            response (<! (http/get url {:with-credentials? true}))
            success (= (response :status) 200)]
        (println ">> " url "\n" response)
        (if success
          (do
            (reset! atom (response :body))
            (js/setTimeout poll 100 path (get-in response [:headers "x-stamp"]) atom))
          (do
            (reset! atom nil)
            (js/setTimeout poll 5000 path 0 atom)))))
    atom))

(defn post
  "Post some data to the server. Sends it as JSON."
  [path body]
  (http/post path {:with-credentials? true
                   :json-params body}))
