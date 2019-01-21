package larkworthy.model

import larkworthy.model.ElevatorSystem.Floor
import squants.{Length, Time}
import squants.motion.{Distance, Velocity}
import squants.space.{Centimeters, Meters}
import squants.time.Seconds

case class ElevatorConfig(floorPositions: Map[Floor, Distance],
                          // Note stepsize * velocity must be less than tolerance otherwise oscillations occur
                          stepSize: Time = Seconds(0.01),
                          moveVelocity: Velocity = Meters(4) / Seconds(1),
                          positionTolerance: Length = Centimeters(5)
                               ) {
  implicit val tolerance = positionTolerance


  def floorForPosition(position: Distance): Option[Floor] =
    floorPositions.find {
      case (_, floorPosition) => position =~ floorPosition
    }.map(_._1)


  def floorsAhead(position: Distance, dir: Direction): Iterable[Floor] =
    dir match {
      case UP => floorPositions.collect {
        case (floor, floorPosition) if floorPosition > position => floor
      }
      case DOWN => floorPositions.collect {
        case (floor, floorPosition) if floorPosition < position => floor
      }
    }
}
