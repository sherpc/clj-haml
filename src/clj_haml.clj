(comment
  "HAML like templating library"
)


;; @author Antonio Garrote Hernandez
;;
;; This is free software available under the terms of the
;; GNU Lesser General Public License
;;

(ns clj-haml)

(defn keyword-to-string
  "Transforms a keyword into a string minus ':'"
  ([kw] (. (str kw) (substring 1 (. (str kw) length)))))

(defmacro h=
  "Main Haml function"
  ([& params]
     (let [elems (if (keyword? (first params))
                   (let [kws (keyword-to-string (first params))]
                     (if (and (= -1 (. kws (indexOf "#")))
                              (= -1 (. kws (indexOf "."))))
                       {:tag kws :class nil :id nil}
                       (if (and (= -1 (. kws (indexOf "#")))
                                (not (= -1 (. kws (indexOf ".")))))
                         (let [parts (. kws (split "\\."))
                               tag (if (= (first parts) "") "div" (first parts))
                               class (loop [classes (rest parts)
                                            class_txt ""]
                                       (if (empty? classes)
                                         class_txt
                                         (recur (rest classes)
                                                (str class_txt " " (first classes)))))]
                           {:tag tag :class class :id nil})
                         (if (and (not (= -1 (. kws (indexOf "#"))))
                                  (= -1 (. kws (indexOf "."))))
                           (let [parts (. kws (split "#"))
                                 tag (if (= (first parts) "") "div" (first parts))
                                 id (second parts)]
                             {:tag tag :class nil :id id})
                           (let [idi (. kws (indexOf "#"))
                                 idc (. kws (indexOf "."))]
                             (if (> idi idc)
                               (let [partsc (. kws (split "\\."))
                                     tag (first partsc)]
                                 (loop [other-classes (rest partsc)
                                        the-classes ""
                                        the-id ""]
                                   (if (empty? other-classes)
                                     {:tag tag :class the-classes :id the-id}
                                     (let [this-class (first other-classes)
                                           this-idi (. this-class (indexOf "#"))]
                                       (if (= -1 this-idi)
                                         (recur (rest other-classes)
                                                (str the-classes " " this-class)
                                                the-id)
                                         (let [this-class-parts (. this-class (split "#"))]
                                           (recur (rest other-classes)
                                                  (str the-classes " " (first this-class-parts))
                                                  (second this-class-parts))))))))
                               (let [partsc (. kws (split "#"))
                                     tag (first partsc)
                                     restc (. (second partsc) (split "\\."))
                                     id (first restc)
                                     class (loop [classes (rest restc)
                                                  class_txt ""]
                                             (if (empty? classes)
                                               class_txt
                                               (recur (rest classes)
                                                      (str class_txt " " (first classes)))))]
                                 {:tag tag :class class :id id}))))))))

           tag (:tag elems)
           id (:id elems)
           class (:class elems)

            attrs (loop [parts params]
                    (if (empty? parts)
                      nil
                      (let [this-part (first parts)]
                        (if (map? this-part)
                          this-part
                          (recur (rest parts))))))
;;                       `[~tag, ~id, ~class, ~attrs])))
            self-closed? (let [last-param (last params)]
                           (if (symbol? last-param)
                             (if (= (str last-param) "/") true false)
                             false))
            content (loop [looking params
                           acum ""]
                      (if (empty? looking)
                        acum
                        (let [maybe-content (first looking)]
                          (if (or (keyword? maybe-content)
                                  (and (symbol? maybe-content)
                                       (= \. (first (str maybe-content))))
                                  (and (symbol? maybe-content)
                                       (= \# (first (str maybe-content))))
                                  (map? maybe-content))
                            (recur (rest looking)
                                   acum)
                            (recur (rest looking)
                                   (str acum (eval maybe-content)))))))
            pre (str "<" tag
                     (when (not (nil? id))
                       (str " id='" id "'"))
                     (when (not (nil? class))
                       (str " class='" (. class (substring 1)) "'"))
                     (loop [attr-keys (keys attrs)
                            acum ""]
                       (if (not (empty? attr-keys))
                         (let [this-key (first attr-keys)
                               this-val (get attrs this-key)]
                           (recur (rest attr-keys)
                                  (str acum " " (keyword-to-string this-key) "='" (eval this-val) "'")))
                         acum)))]
        (if self-closed?
          (str pre "/>")
        (str pre ">" content "</" tag ">")))))


(defn h-file
  "Loads and parses a Haml template"
  ([path]
     (load-file path)))

(defmacro with-haml-template [template format & bindings]
  `(binding [~@(first bindings)]
     (apply h-file [(str ~template "." (keyword-to-string ~format) ".haml")])))

(defmacro !!! [& args]
  (if (= :xml (first args))
    (if (string? (second args))
      `(str "<?xml version='1.0' encoding='" ~(second args) "' ?>"
           ~@(drop 2 args))
      `(str "<?xml version='1.0' encoding='utf-8' ?>"
           ~@(rest args)))
    (if (float? (first args))
      `(str "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML " ~(first args) " Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
            ~@(rest args))
    (if (= "Strict" (str (first args)))
      `(str "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
            ~@(rest args))
      `(str "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
            ~@args)))))