# multiguess-selection
Clojure demo of selection of Push programs based on "many guesses"

Individuals are assigned multiple "fitnesses" based on the numbers contained in their entire `:integer` stack at evaluation. "Objectives" are similarity to a target value, and "depth" of each number in their stacks. Non-dominated values are _all_ retained as their "guesses".

Ranking of individuals is implemented using non-domination comparisons.
