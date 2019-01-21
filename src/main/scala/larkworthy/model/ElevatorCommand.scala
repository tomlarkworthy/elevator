package larkworthy.model

import larkworthy.model.ElevatorSystem.CarId
import squants.Velocity

case class ElevatorCommand(carCommands: Map[CarId, CarCommand])
sealed trait CarCommand
case class CloseDoor(dir: Direction) extends CarCommand
case class OpenDoor(dir: Direction) extends CarCommand
case class Move(velocity: Velocity) extends CarCommand
case object SwitchDir extends CarCommand
case object NoCommand extends CarCommand

