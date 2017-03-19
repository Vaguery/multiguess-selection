# multiguess-selection
Clojure demo of selection of Push programs based on "many guesses"

Individuals are assigned multiple "fitnesses" based on the numbers contained in their entire `:integer` stack at evaluation. "Objectives" are similarity to a target value, and "depth" of each number in their stacks. Non-dominated values are _all_ retained as their "guesses".

Ranking of individuals is implemented using non-domination comparisons.

## standard Push selection

In Push program evolution, for many years it's been  traditional to treat the "top item on stack X at the time limit" as an implicit return value. That is, if we are looking for an `:integer`-valued function, we run each program until "done" (or a time limit is met), and `pop` the top `:integer` item as the "result".

Scoring is done by comparing this value to the _expected_ value for the given training case. If there are no `:integer` items on that stack after some program is run, then to produce a valid numeric score a large "penalty" is usually applied instead.

## Push programs provide many "guesses"

Unlike tree-based genetic programming representations, Push programs end up with an entire _stack_ of values. Thus, the standard selection approach in some real way implies not only that (1) we want the correct number, but also that it should be (2) on top of the stack (with no items above it), (3) at the correct point in time.

Consider the case of a _nearly correct_ program, which always produces an answer at the top of the `:integer` stack when checked which is always off by `1`, for all training cases. There could be several other `:integer` values present below that top number, but we rarely if ever consider those.

Assume that "nearly correct" program is selected for recombination, and that two things occur in the same event: (1) some constant is mutated, so the calculation becomes _always_ correct, but also (2) a constant `99` is appended to the tail of the "absolutely correct" program by recombination. That is, the _perfect_ answer will now appear _second_ in the `:integer` stack, and the constant `99` will appear at the top of the stack for every training case.

This is bad. If the "nearly correct" program was the closest we had ever gotten to an answer, and it was replaced completely by this new "masked" `99`-answering offspring, then all of the evolutionary search that went into finding a perfect answer may end up being wasted. Despite the fact that the perfect answer is sitting right there, second in the `:integer` stack.

## rethinking "top of the stack"

I said above that the traditional approach of looking only at the top item on a stack as an implicit answer implies:

1. we want the number to match the target value
2. we want that number at the top of that specific stack
3. we want that number to be there at a particular "time" (for example, when the program is complete, or 3000 steps, whichever is first)

That is, if a program's `:integer` stack is `'(1 2 3 4 5 6)` (with `1` at the "top"), and the correct answer is `3`, then the "traditional answer" will be `1`, giving an absolute error of `2` points.

But notice that `3` is _also_ on that same stack. Its absolute error is `0`, but it's not at the top of the stack: it's two steps down.

What happens if we look at the _entire_ `:integer` stack, and for each value present we record its error (absolute difference from the target value), and also its depth in the stack? Let me use a slightly more complicated stack: `'(7 2 3 1 0 11)`, and suppose our target value is `10`

~~~ text
value   depth   |error|
7         0        3
2         1        8
3         2        7
1         3        9
0         4        10
11        5        1
~~~

Which of these `[depth error]` vectors is "best"? Clearly, it's the ones with the smallest error _and_ the smallest depth.

In this case, there is no single `[depth error]` vector where both values are smaller than the respective values in all the other vectors.

Instead, let's find the nondominated vectors in this collection of six. One vector _dominates_ another, in this setting, if _both_ `error` and `depth` are at least as small, and either `error` or `depth` is strictly smaller.

For example, `[1 2]` dominates `[2.2 3.3]`, because `(1 ≤ 2.2)` and `(2 ≤ 3.3)`, and _also_ `(1 < 2.2)`.

Notice that `[1 2]` also dominates `[2 2]`, because `(1 ≤ 2)` and `(2 ≤ 2)`, and _also_ `(1 < 2)`.

But `[1 2]` does _not_ dominate `[2.2 1.1]`, because `(2 > 1.1)`.

And (for completeness), does `[1 2]` dominate `[1 2]`? No. Both `(1 ≤ 1)` and `(2 ≤ 2)`, but neither `(1 < 1)` nor `(2 < 2)`.

So looking at our example of six numbers above, which are _non-dominated_ with respect to all the others?

~~~ text
value  |error|   depth     dominated by
7         3        0       -
2         8        1       7
3         7        2       7
1         9        3       7,2,3
0        10        4       7,2,3,1
11        1        5       -
~~~

That is, the nondominated values in this stack `'(7 2 3 1 0 11)` are `7` and `11`. All the others have `[depth error]` vectors that are dominated by at least one other number's `[depth error]` vector.

### ranking programs by their "best guesses"

Suppose we call _all_ the numbers in the `:integer` stack at the time we check "guesses". And we call these two nondominated numbers in the `:integer` stack this program's "best guesses". None of the other numbers dominate these in terms of both `depth` and `error`.

Record this program's best guesses as `#{[0 3] [5 1]}`. I don't really care about their order, so I've put them in a Clojure `set` here.

If you think about it, as long as there's an number at all on the `:intger` stack, the first number present will be one of that program's best guesses. No other value can have a smaller `depth`, after all, so it will certainly be nondominated.

But now, if there are deeper numbers that are closer to the target value, we may have more best guesses beyond that "top" number.

And for completeness, let's say that any program that has _nothing_ on its `:integer` stack when we check has simply not made any guesses at all. Its "best guesses" set is `#{}`.

Can we compare between programs, using these sets of best guesses?

Of course.

Here's one way (I haven't optimized the algorithm, so the purpose remains clear):

1. exercise every program with the same training case: set up inputs, run the program, examine the `:integer` stack at the end, and now record the `best-guesses` for the specified target value, as `[depth error]` vectors
2. find the set of "best `best-guesses`" for all the programs collectively; that is, pool all their `best-guesses` sets, and find only the vectors which are nondominated in that pool
3. set aside all the programs which have one of the _collectively_ nondominated guess vectors in their personal `best-guesses` sets
4. go back to `2`, using only the remaining programs, repeatedly removing the "best best guessers" from each subset, until everybody is set aside


What we've done here has produced a clear ranking of the programs: The first ones we set aside are those have which, collectively, guessed at least one _global_ nondominated number. Once we've set them aside, we repeat the process with the remaining ones; there will be a new set of "best bests" for this subset, and we can peel off another layer of the onion based on those winning guesses. And so on. Eventually, we'll be left with a tiny collection of programs where everybody has at least one "best best guess", and we're done.

Notice an interesting side-effect of this process: somebody else will be left in the very last pile, too. The programs with _empty_ `:integer` stacks will never have made any guesses at all, so they'll be automatically included in the lowest rank.

## An example

This repository implements a few Clojure functions that aim to make this clearer.

If you look at the Midje tests, you'll see one last test that is set up to fail... but in an informative way. Here's the result of running this once, cleaned up a bit. The test is building 20 random `Individual` records, each with a random `:stack` containing a random collection of up to 20 random integers in the range `[0,100)`. We're looking at a `ranked-individuals`, given a _target_ value of `13`.

That is, all 20 individuals are given `best-guesses` sets, based on how deep the numbers are on their stack, and how close to `13` they are. These are pooled, and the resulting subset of "winners" is extracted and labeled `0`. This process is repeated, removing "deeper" layers of `Individual` records, each of which are non-dominated within their subset.

~~~ clojure
{0 [
  {:genome 10,
    :guesses #{[0 810] [1 648] [2 69] [3 5]},
    :stack (823 661 82 18 849)}
  {:genome 64,
    :guesses #{[0 88] [3 15]},
    :stack (101 690 736 28 53)}],
1 [
  {:genome 55,
    :guesses #{[0 362] [1 119]},
    :stack (375 132 871 414 554 667 272 567)}
  {:genome 55,
    :guesses #{[0 135] [2 110] [7 11]},
    :stack (148 457 123 636 593 601 416 2 642)}
  {:genome 49,
    :guesses #{[0 240] [5 106]},
    :stack (253 315 392 959 344 119 958 910)}],
2 [
  {:genome 22,
    :guesses #{[0 841] [1 387] [7 98]},
    :stack (854 400 400 901 489 859 972 111 584)}
  {:genome 19,
    :guesses #{[0 867] [1 787] [3 560] [4 150]},
    :stack (880 800 925 573 163 574 849 819)}
  {:genome 47,
    :guesses #{[0 201]},
    :stack (214 724 644)}
  {:genome 39,
    :guesses #{[0 653] [1 295] [2 151]},
    :stack (666 308 164 242 346 791 860)}
  {:genome 36,
    :guesses #{[0 662] [1 163]},
    :stack (675 176 744 664 354 587)}],
3 [
  {:genome 27,
    :guesses #{[0 290]},
    :stack (303 527)}
  {:genome 40,
    :guesses #{[0 873] [2 472] [4 469] [6 259] [7 223]},
    :stack (886 983 485 952 482 846 272 236 474)}],
4 [
  {:genome 66,
    :guesses #{[0 336]},
    :stack (349 462 460)}],
5 [
  {:genome 93,
    :guesses #{[0 439]},
    :stack (452 633)}
  {:genome 37,
    :guesses #{[0 905] [1 481] [3 364]},
    :stack (918 494 642 377 910)}],
6 [
  {:genome 15,
    :guesses #{[0 895] [1 625] [3 413]},
    :stack (908 638 758 426 767)}
  {:genome 78,
    :guesses #{[0 636]},
    :stack (649)}
  {:genome 27,
    :guesses #{[0 743] [2 465]},
    :stack (756 959 478)}],
7 [
  {:genome 55,
    :guesses #{},
    :stack ()}
  {:genome 33,
    :guesses #{},
    :stack ()}]}
~~~

What can we see here?

The 20 `Individual` records separated into 8 layers. Let's look at the extremes first.

In the "lowest" layer, we see the two that had empty stacks.

In the "top" layer, we see the two that had stacks containing nondominated values _over the entire collection_. The two `Individual` records here are `{:genome 10, :guesses #{[0 810] [1 648] [2 69] [3 5]}, :stack (823 661 82 18 849)}` and `{:genome 64, :guesses #{[0 88] [3 15]}, :stack (101 690 736 28 53)}`. Like I said, _within_ each individual's `:guesses`, the top item on the `:stack` appears every time. Notice that the first one of these "winners" has a `:guess` vector `[3 5]`, mapping to the `:stack` item `18` in its fourth position. The second has a `:guess` of `[0 88]`—the lowest-valued topmost number on _any_ `:stack`.

Makes sense, doesn't it?

We should expect that each layer will include the `Individual` with the smallest value on the top of its `:stack` (compared to the remaining ones), and also an `Individual` with the smallest error remaining in _any_ position on a `:stack`. These could be the same `Individual`.

But there is also room in each layer for mutually non-dominated guessers. Look at the records in layer `2` here: The one labeled `:genome 47` has the lowest-valued top number. The one labeled `:genome 22` has the lowest-valued error anywhere. The others each have reasonably-low-error, reasonably-low-depth scores, which balance one another out.

## concerns

There seems to be an awful lot of extra computational work going on here. Determining the sets of vectors, comparing between them, recursively removing "layers"... it all feels like it will add up. Is the expense worth the trouble?

That depends.

For problems traditionally called "easy", of course not. But realize that those problems are _called_ "easy" because they don't need this extra work. Does an approach like this guarantee that a search that _isn't_ "easy" becomes easier?

Dunno.

You might be concerned that there is an implicit bias here to select for "lots of guesses". That is, for programs that produce a big pile of stack items, in hopes of hitting it big with a lucky guess. But think this through: If these numbers are _random_, then they can't hold up to repeated scrutiny. And if they're _arbitrary_, then there's no way for them to be "lucky" unless that luck contains at least a little bit of a model of the target distribution... at least under evolutionary selection. It seems like these "multi-guess" approaches are no more prone to "cheating" than traditional approaches are prone to evolving solutions that "guess the right answer".

You'll recall that I said not to worry about _when_ a value appears on the stack, the "third" implicit objective of the traditional approach. In this example, I've just elided that. The same approach can make selection "even more gentle", by moving from `[depth error]` vectors to three-dimensional `[depth last-appearance error]` vectors. Here `last-appearance` represents that program execution step _before_ the "deadline" where a given value past appeared. So for example if a program gets the right answer on the next-to-last step _but then deletes it_, there can be selection pressure brought to bear to "fix the reversion bug" if the "right" value can be seen in the history of the salient stack.

## Related things

Una-May O'Reilly and Krzysztof Krawiec have done some work with tree-based GP representations, looking at "partial solutions" present in sub-tree expressions. This work differs somewhat, just because of the different nature of Push vs tree-based GP interpreter structure. That said, the goals and sentiment are closely related: to make a "steep" landscape more amenable to gradual hill-climbing.
