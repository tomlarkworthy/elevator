Elevator Simulator
==================

This is my solution to a coding challenge set during an interview process.

The top level interface is [larkworthy.hci.ElevatorController](src/main/scala/larkworthy/hci/ElevatorController.scala)
which is an evolution of the suggested elevator control interface.

I added two types of elevator requests:
1, an ExternalRequest is one made by someone standing on a floor request an
elevator to take them somewhere, they indicate the direction (either up or down);
2, an InternalRequest is made by someone already in an specific elevator car indicating where they wish to get off.

Functional Core
---------------
Personally, I do not like stateful OO interfaces, so the bulk of the work was done in
[larkworthy.hci.ElevatorSystem](src/main/scala/larkworthy/model/ElevatorSystem.scala) which is a immutable elevator
control system.

Case classes for data. Sealed classes for enumerations. Immutable collections for containers. "Mutation" is achieved by
forking a new immutable copy.

I value simplicity over performance, so there are many linear searches that _could_ be sped up with more complex
indexing (e.g. pruning fullfilled requests). I consider making the system performant a secondary issue that should not
be done until its shown to be a problem and I have a profiler in hand.

Physical Units
--------------
The elevator states, commands and step size are described in type safe physical units (distance, velocity, time), which
makes simulations much more easily interpreted.

I used squants, though I would have preferred not to use doubles.

See [larkworthy.model.ElevatorState](src/main/scala/larkworthy/model/ElevatorState.scala)
and [larkworthy.model.ElevatorCommand](src/main/scala/larkworthy/model/ElevatorCommand.scala)

A highlight is that the stopping tolerance is expressed as centermeters(5), and move velocity is Meters(4) / Seconds(1)
which is a common top speed for typical elevators.

Randomized Property Tests
-------------------------
I have not bothered many unit tests. Usually they essential for development velocity and documentation. But due to short
timeframe I wanted to ensure correctness over process. In my experience, correctness is easier to demonstrate using
randomized property testing. I used ScalaCheck. It caught several non-transitive floating point mistakes and incorrect
step size induced bugs.

The core idea of the correctness property is that we have an issue when the system starts repeating itself when trying to serve requests.
So we generate a random state with random requests, then check that eventually all the the requests are server. We can abort property 
checking with a failure when we see the same state twice.

See [larkworthy.model.ElevatorSystemSpec](src/test/scala/larkworthy/model/ElevatorSystemSpec.scala)

LOOK algorithm
--------------

I researched the scheduling problem before implementing it, and for embedded control you want a soft realtime control
algorithm that runs in bounded time (i.e. don't invoke a Mixed Integer Programming solver).

For elevators specifically you don't necessarily want the optimal minimization of average wait time, but you want
something that does not have long tail latencies. Everyone should be treated somewhat fairly. 

A good solution for elevators is the [LOOK algorithm](https://en.wikipedia.org/wiki/LOOK_algorithm) that keeps the
elevator moving in the same direction until it runs out of requests that could be fulfilled ahead of it.
It then reverses direction. This has shown to have good tail latency performance while avoiding starvation.

See [larkworthy.solver.LookSolver](src/main/scala/larkworthy/solver/LookSolver.scala)

Running it
----------
For convenience, the spec test is run in a JUnit test, so

`mvn test`

will run a 1000 random setups and check all requests get fullfilled.

Note: I don't specs in unit tests normally, as unit testing should be very fast.