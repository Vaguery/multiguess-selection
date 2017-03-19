(ns multiguess-selection.core-test
  (:use midje.sweet)
  (:use [multiguess-selection.core]))

(fact "I can make an Individual record"
  (:genome (->Individual 1 [] [])) => 1
  (:stack (->Individual 2 [2 3] [])) => [2 3]
  (:guesses (->Individual 3 [1 8] #{[1 8]})) =>
    #{[1 8]}
  )

(fact "depth-score-pairs converts a stack and a target into [position abs-error] vectors"
  (depth-score-pairs '(3) 2) => [[0 1]]
  (depth-score-pairs '(2) 2) => [[0 0]]
  (depth-score-pairs '(7 1) 2) => [[0 5] [1 1]]
  (depth-score-pairs '(7 1) 7) => [[0 0] [1 6]]
  (depth-score-pairs '(2 2) 2) => [[0 0] [1 0]]
  (depth-score-pairs '() 2) => []
  )


(fact "dominates? predicate"
  (dominates? [0 0] [1 1]) => true
  (dominates? [2 2] [1 1]) => false
  (dominates? [1 2] [2 1]) => false
  (dominates? [1 2] [1 2]) => false
  (dominates? [1 2] [1 3]) => true
  (dominates? [1 3] [1 2]) => false
  )


(fact "best-guesses returns the set of non-dominated depth-score-pairs from a stack and a target value"
  (best-guesses '(2 2) 2) => #{[0 0]}
  (best-guesses '(1 2) 2) => #{[0 1] [1 0]}
  (best-guesses '(5 4 3 2 1) 9) =>
    #{[0 4]}
  (best-guesses '(5 4 3 2 1) 5) =>
    #{[0 0]}
  (best-guesses '(5 4 3 2 1) 4) =>
    #{[0 1] [1 0]}
  (best-guesses '(5 4 3 2 1) 3) =>
    #{[0 2] [1 1] [2 0]}
  (best-guesses '(5 4 3 2 1) 0) =>
    #{[0 5] [1 4] [2 3] [3 2] [4 1]}
  (best-guesses '(5 4 5 4 5) 0) =>
    #{[0 5] [1 4]}
    )


(fact "score-individual takes an Individual record with stack, and a target, and fills its :guesses with a best-guesses set"
  (let [dude (->Individual 1 '(8 2 9 1 4 0) [])]
    (:guesses (score-individual dude 1)) => #{[0 7] [1 1] [3 0]}
    (:guesses (score-individual dude 2)) => #{[0 6] [1 0]}
    (:guesses (score-individual dude 3)) => #{[0 5] [1 1]}
    (:guesses (score-individual dude 4)) => #{[0 4] [1 2] [4 0]}
    ))


(fact "best-overall-guesses takes a collection of Individual records, all with best-guesses set, and returns the mutually non-dominated guess vectors"
  (best-overall-guesses
    [(score-individual (->Individual 1 '(0 1 2) #{}) 1)
    (score-individual (->Individual 2 '(1 2 0) #{}) 1)]) => #{[0 0]}
  (best-overall-guesses
    [(score-individual (->Individual 1 '(0 1 2) #{}) 3)
    (score-individual (->Individual 2 '(1 2 0) #{}) 3)]) => #{[0 2] [1 1]}
  (best-overall-guesses
    [(score-individual (->Individual 1 '(0 1 2) #{}) 7)
    (score-individual (->Individual 2 '(0 1 1) #{}) 7)]) => #{[0 7] [1 6] [2 5]}
  (best-overall-guesses
    [(score-individual (->Individual 1 '(0 1 2) #{}) 1)]) => #{[0 1] [1 0]}
  (best-overall-guesses []) => #{}
  )


(fact "I can rank a collection of Individuals using ranked-individuals"
  (let [pop (repeatedly 20
              #(score-individual
                (->Individual
                  (rand-int 100)
                  (repeatedly 20 (fn [] (rand-int 1000)))
                  #{})
                13))]
    (ranked-individuals
      [(score-individual (->Individual 1 '(0 1 2) #{}) 3)
       (score-individual (->Individual 2 '(1 2 0) #{}) 3)]) => {0
            [{:genome 2, :guesses #{[0 2] [1 1]}, :stack '(1 2 0)}],
          1
            [{:genome 1, :guesses #{[0 3] [1 2] [2 1]}, :stack '(0 1 2)}]}
    (ranked-individuals pop) => 99 ;; intentional fail
    ))
