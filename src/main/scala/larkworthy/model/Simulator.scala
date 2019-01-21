package larkworthy.model

import squants.Time
import squants.motion.Distance
import squants.space.{Meters, Millimeters}
import squants.time.Seconds

import scala.collection.parallel.CompositeThrowable
import scala.util.{Failure, Success, Try}

/**
  * The simulator takes a command and applies it to the state to generate a new state which represents what we
  * beleive would happen. We also add some saftey checks to defensively protect against incoherent commands such
  * as trying to open a door twice. The idea of these checks is that the simulator can check for clear mistakes in the
  * command solver implementations.
  */
object Simulator {

  def roundToMillis(distance: Distance): Distance = Millimeters(Math.round(distance.toMillimeters))

  def step(car: CarState, command: CarCommand, t: Time): Try[CarState] = {
    def error(msg: String) = Failure(new IllegalStateException(msg))
    (command, car) match {
      case (OpenDoor(_), CarStateOpen(_, _)) =>
        error("Door already open")
      case (OpenDoor(dC), CarStateMoving(p, dS)) if dC == dS =>
        Success(CarStateOpen(p, dC))
      case (OpenDoor(dC), CarStateMoving(_, dS)) if dC != dS =>
        error("Car must move in advertised direction")

      case (CloseDoor(dC), CarStateOpen(p, dS)) if dC == dS =>
        Success(CarStateMoving(p, dC))
      case (CloseDoor(dC), CarStateOpen(_, dS)) if dC != dS =>
        error("Car must move in advertised direction")
      case (CloseDoor(_), CarStateMoving(_,_)) =>
        error("Door not open")

      case (Move(_), CarStateOpen(_, _)) =>
        error("Door not closed")
      case (Move(v), CarStateMoving(p, dir)) =>
        dir match {
          case UP if v < Meters(0) / Seconds(1) =>
            error("Car moving UP requires positive velocity")
          case DOWN if v > Meters(0) / Seconds(1) =>
            error("Car moving DOWN requires negative velocity")
          case _ => Success(CarStateMoving(roundToMillis(p + v * t), dir))
        }

      case (NoCommand, state) => Success(state)

      case (SwitchDir, CarStateMoving(p, dir)) => Success(CarStateMoving(p, dir.opposite))
      case (SwitchDir, CarStateOpen(p, dir)) => Success(CarStateOpen(p, dir.opposite))
    }
  }


  def step(elevator: ElevatorState, command: ElevatorCommand, t: Time): Try[ElevatorState] = {
    val carStates = elevator.cars.map {
      case (carId, car) =>
        val carCommand = command.carCommands.getOrElse(carId, NoCommand)
        (carId, step(car, carCommand, t))
    }

    val errors = carStates.values.collect {
      case Failure(e) => e
    }.toSet

    // If a single car step fails, this is logic error, so we fail the whole operation
    // We collect all the errors up into a single CompositeError to help debug the issue
    // But we expect to always return success.
    // In a real system perhaps we should let other cars work normally if one breaks?
    if (errors.nonEmpty) {
      Failure(CompositeThrowable(errors))
    } else {
      val successStates = carStates.map {
        case (carId, tryState) => (carId, tryState.get)
      }
      Success(ElevatorState(successStates))
    }
  }
}
