package larkworthy.model

import org.junit.Assert._
import org.junit.Test
import squants.space.Meters
import squants.time.Seconds

import scala.util.Success

@Test
class SimulatorTest {

  /**
    * Two cars, send one up and send one down.
    * Check the lifts are moving in the right direction
    */
  @Test
  def accelerationPolarity(): Unit = {
    val model = ElevatorState(Map(
      1 -> CarStateMoving(Meters(0), UP),
      2 -> CarStateMoving(Meters(0), DOWN)
    ))

    val command = ElevatorCommand(Map(
      1 -> Move(Meters(1) / Seconds(1)),
      2 -> Move(Meters(-2) / Seconds(1))
    ))

    val newState = Simulator.step(model, command, Seconds(1))

    assertEquals(
      Success(ElevatorState(Map(
        1 -> CarStateMoving(Meters(1), UP),
        2 -> CarStateMoving(Meters(-2), DOWN)
      ))),
      newState
    )
  }
}
