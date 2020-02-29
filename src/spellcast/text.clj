(ns spellcast.text
  (:require [antlers.core :refer [render-string]]))

(def pronouns-map
  {:they {:sub "they" :obj "them" :pos "their" :ref "themself"}
   :she  {:sub  "she" :obj "her"  :pos "her"   :ref "herself"}
   :he   {:sub  "he"  :obj "him"  :pos "his"   :ref "himself"}
   :it   {:sub  "it"  :obj "it"   :pos "its"   :ref "itself"}})

(defn- prn [player context]
  (get-in pronouns-map [(player :pronouns) context]))

(defn msg [text & {:as bindings}]
  (render-string text (assoc bindings :prn prn :get get)))
