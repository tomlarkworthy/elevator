package larkworthy.model

import larkworthy.model.ElevatorSystem.CarId
import squants.motion.Distance

/**
  * Physical state of the elevator, where the cars are, what direction they are travelling, and whether their doors
  * are open or closed.
  * @param cars map for CarId to CarState
  */
case class ElevatorState(cars: Map[CarId, CarState])

sealed trait CarState
case class CarStateMoving(position: Distance, dir: Direction) extends CarState
case class CarStateOpen(position: Distance, dir: Direction) extends CarState

sealed abstract class Direction(val sign: Int) {
  def opposite: Direction = this match {
    case UP => DOWN
    case DOWN => UP
  }
}
case object UP extends Direction(1)
case object DOWN extends Direction(-1)




