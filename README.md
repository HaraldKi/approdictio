# approdictio --- Approximate Dictionaries
**a Java class library**


Approdictio provides implementations of dictionaries that allow
approximate lookup. When looking up a word, all words are returned
that are approximately equal to the word given. The definition of
approximately equal depends on the
[metric](http://en.wikipedia.org/wiki/Metric_(mathematics)) provided
to define the distance between two words. An implementation of the
[Levensthein
Metric](http://en.wikipedia.org/wiki/Levenshtein_distance) with
customizable edit costs is part of the package.

The slightly tricky part of approximate dictionary lookup is to find all similar words without explicit comparison of the word to look up with all words of the dictionary. Two implementations are provided that are reasonably fast:

### BKTree 

provides an implementation of a [Burkhard-Keller Tree](http://en.wikipedia.org/wiki/BK-tree). In
principle, this implementation can even be used for approximate lookup
of other objects than strings, as long as a metric is provided.

###NgramDict

indexes all words of the dictionary by their n-grams. During lookup,
candidate terms are retrieved with a crude n-gram similarity. Only the
candidates are compared according to the metric provided. This seems
generally to be faster than the BKTree, but has the disadvantage that
even the Levensthein metric is not 100% compatible with the n-gram
lookup. Consequently some similar terms may be missed.