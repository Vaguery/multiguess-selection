(ns multiguess-selection.core)

(defrecord Individual [genome stack guesses])


(defn abs-error
  "returns the absolute difference between two numbers"
  [n1 n2]
  (Math/abs (- n1 n2)))


(defn depth-score-pairs
  "given a collection ('stack') and a target value, maps this into a collection of vectors where the first item is the depth in the original stack, and the second the absolute error of the item vs the target value"
  [stack target]
  (let [diffs (map (partial abs-error target) stack)]
    (reduce-kv
      (fn [new k v] (conj new [k v]))
      []
      (into [] diffs))
      ))


(defn dominates?
  "returns `true` if v1 strictly dominates v2 (both values are at least as small, and one or both are smaller)"
  [v1 v2]
  (boolean
    (and
      (every? true? (map <= v1 v2))
      (some true? (map < v1 v2))
      )))


(defn non-dominating-pair?
  "returns `true` if neither v1 or v2 dominates the other"
  [v1 v2]
  (not (or
    (dominates? v1 v2)
    (dominates? v2 v1))))


(defn non-dominated-set
  "given a collection of vectors, returns the subset of the vectors which are mutually non-dominating"
  [vectors]
  (reduce
    (fn [bests v]
      (if (some #(dominates? % v) bests)
        bests
        (if (some #(dominates? v %) bests)
          (conj
            (into #{} (remove #(dominates? v %) bests))
            v)
          (conj bests v))))
    #{}
    vectors
    ))


(defn best-guesses
  "given a stack of numbers and a target, returns the non-dominated subset of items in the stack, using both depth and error (as depth-score-pairs)"
  [stack target]
  (non-dominated-set
    (depth-score-pairs stack target)))


(defn score-individual
  "given an Individual record with a stack in it and a target number, assigns the Individual's `:guesses` field to be the subset of non-dominated items in the stack"
  [individual target]
  (assoc individual
         :guesses
         (best-guesses (:stack individual) target)))


(defn best-overall-guesses
  "given a collection of `Individual` records (with `:guesses` assigned), returns the set of non-dominated guess vectors appearing anywhere in the entire collection"
  [individuals]
  (non-dominated-set
    (reduce
      #(into %1 (:guesses %2)) #{} individuals)))


(defn extract-best-guessers
  "given a collection of individuals, this returns only those individuals whose `:guesses` contain collectively-nondominated vectors"
  [individuals]
  (let [bests (best-overall-guesses individuals)]
    (group-by #(not-any? bests (:guesses %)) individuals)
    ))


(defn ranked-individuals
  "given a collection of Individuals, this recursively partitions the collection in 'layers' of non-dominated vs dominated. The key is the 'degree' of domination, taking each layer out in turn and recalculating"
  [individuals]
  (loop [unsorted individuals
         counter 0
         ranks {}]
    (let [parts (extract-best-guessers unsorted)]
      (if (empty? (get parts true))
        (assoc ranks counter (get parts false))
        (recur (get parts true)
               (inc counter)
               (assoc ranks counter (get parts false))
               )))))
