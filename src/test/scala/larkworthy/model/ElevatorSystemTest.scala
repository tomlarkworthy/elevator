package larkworthy.model

import larkworthy.model.ElevatorSystem.{ExternalRequest, InternalRequest}
import larkworthy.solver.LookSolver
import org.junit.Assert._
import org.junit.Test
import org.scalacheck.Test.{Parameters, Passed}
import org.scalacheck.util.ConsoleReporter
import squants.space.Meters
import org.scalacheck.{Test => SchkTest}

@Test
class ElevatorSystemTest {
  val twoCarFiveFloors = ElevatorConfig(
    (0 to 5).map(floor => floor -> floor * Meters(10)).toMap
  )

  val twoCarsOpenUp = ElevatorState(
    (1 to 2).map(id => id -> CarStateOpen(Meters(10), UP)).toMap
  )

  @Test
  def smokeExternals() {
    var system = ElevatorSystem(
      twoCarFiveFloors,
      twoCarsOpenUp,
      Set(ExternalRequest(0, UP), ExternalRequest(2, DOWN)),
      Set.empty,
      LookSolver
    )

    var seenOpenCarOn2 = false
    var seenOpenCarOn0 = false

    while(system.externalRequests.nonEmpty) {
      system = system.step().get

      if (system.hasOpenCarOn(2)) seenOpenCarOn2 = true
      if (system.hasOpenCarOn(0)) seenOpenCarOn0 = true
    }

    assertTrue(seenOpenCarOn0)
    assertTrue(seenOpenCarOn2)
  }

  @Test
  def smokeInternals() {
    var system = ElevatorSystem(
      twoCarFiveFloors,
      twoCarsOpenUp,
      Set.empty,
      Set(InternalRequest(1, 1), InternalRequest(1, 5), InternalRequest(2, 0), InternalRequest(2,2)),
      LookSolver
    )

    var seenOpenCarOn = (0 to 5).map(floor => floor -> false).toMap

    while(system.internalRequests.nonEmpty) {
      system = system.step().get

      for (floor <- 0 to 5) {
        if (system.hasOpenCarOn(floor)) {
          seenOpenCarOn = seenOpenCarOn + (floor -> true)
        }
      }
    }

    assertTrue(seenOpenCarOn(0))
    assertTrue(seenOpenCarOn(1))
    assertTrue(seenOpenCarOn(2))
    assertFalse(seenOpenCarOn(3))
    assertFalse(seenOpenCarOn(4))
    assertTrue(seenOpenCarOn(5))
  }

  /**
    * Tests the ElevatorSystemSpec using ScalaCheck. Normally I would not use ScalaCheck in a unit test (too slow)
    * but it simplifies the running instructions if we allow it in the Maven test lifecycle
    * @see ElevatorSystemSpec
    */
  @Test
  def alwaysSolvesPropertySpec(): Unit = {
    assertEquals(Passed, SchkTest.check(
      Parameters.default
          .withMinSuccessfulTests(1000)
          .withTestCallback(ConsoleReporter(1)),
      ElevatorSystemSpec.nonPeriodic).status)
  }
}
