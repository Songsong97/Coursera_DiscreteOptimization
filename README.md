# My solution for the Coursera course "Discrete Optimization"

This repo contains solutions for 5 homework assignments of ["Discrete Optimization" on Coursera by Professor Pascal Van Hentenryck](https://www.coursera.org/learn/discrete-optimization). The implementation of the solver is written in Java, though the course itself uses Python to submit the solution.

## Table of contents
1. [Knapsack](#Chapter1)
2. [Coloring](#Chapter2)
3. [Traveling Salesman Problem](#Chapter3)
4. [Facility](#Chapter4)
5. [Vehicle Routing](#Chapter5)

<a name="Chapter1"></a>
## Knapsack
[Description](./knapsack/handout.pdf)

This problem is non-trivial. Even if you can solve it using dynamic programming, it will deplete the memory when the input scale is large. Here I used heuristic search to get the solution.

First, we sort the items by "value per weight". This pushes our solver closer to the global optimum, which will generate a useful **bounding** at an early stage. It also makes it easier for us to compute the bounding.

The bounding here means the largest possible value we can get by seleting a subset of the items, even with some relaxation of the condition. Thus the bounding is the upper bound of the value we can get under a certain configuration, but not the supremum of that. Here when we are computing the bounding, we assume we can get a fraction of each item. In fact, using a greedy strategy, only the item with the lowest "value per weight" will be chosen partially.

Then, we begin searching the solution space. This process is similar to seaching a tree. At each node, we determine whether or not to add the item to the knapsack. When the current bounding is worse than the best solution we get so far, we prune the tree and backtrack. Below is an example showing the process of the search.

![](./images/knapsack.jpg)

<a name="Chapter2"></a>
## Coloring
[Description](./coloring/handout.pdf)


<a name="Chapter3"></a>
## Traveling Salesman Problem
[Description](./tsp/handout.pdf)

<a name="Chapter4"></a>
## Facility
[Description](./facility/handout.pdf)

<a name="Chapter5"></a>
## Vehicle Routing
[Description](./vrp/handout.pdf)
