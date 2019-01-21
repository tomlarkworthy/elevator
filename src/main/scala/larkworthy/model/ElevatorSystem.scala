package larkworthy.model

import larkworthy.model.ElevatorSystem.{CarId, ExternalRequest, Floor, InternalRequest}
import squants.motion.Distance

import scala.util.Try

/**
  * The ElevatorSystem decorates pending requests to the physical model, a solver for deciding what cars should do,
  * and calls out to a simulator for testing.
  */
object ElevatorSystem {
  type Floor = Int
  type CarId = Int

  case class ExternalRequest(floor: Floor, direction: Direction)
  case class InternalRequest(carId: CarId, floor: Floor)

  /**
    * Remove ExternalRequests that have been fulfilled because some car is open on the right floor
    */
  private def
  pruneExternalRequest(config: ElevatorConfig,
                                   elevator: ElevatorState,
                                   externalRequests: Set[ExternalRequest])
                                  (implicit tolerance: Distance): Set[ExternalRequest]  =
    externalRequests.filter {
      case ExternalRequest(floor, dirReq) => !elevator.cars.values.exists {
        case CarStateOpen(position, dirCar) if Option(floor) == config.floorForPosition(position) && dirCar == dirReq => true
        case _ => false
      }
    }

  /**
    * Remove ExternalRequests that have been fulfilled because the specific car is open on the right floor
    */
  private def pruneInternalRequest(config: ElevatorConfig,
                                   elevator: ElevatorState,
                                   internalRequests: Set[InternalRequest])
                                  (implicit tolerance: Distance): Set[InternalRequest] =
    internalRequests.filter {
      case InternalRequest(carId, floor) => {
        val car = elevator.cars.getOrElse(carId,
          throw new AssertionError("InternalRequest by no existing car"))
        car match {
          case CarStateOpen(position, _) => !(Option(floor) == config.floorForPosition(position))
          case _ => true // Keep InternalRequest pending, this car is not at the right stop
        }
      }
    }
}

case class ElevatorSystem(config: ElevatorConfig,
                          elevator: ElevatorState,
                          externalRequests: Set[ExternalRequest],
                          internalRequests: Set[InternalRequest],
                          solver: ElevatorSystem => ElevatorCommand) {


  implicit val tolerance = config.positionTolerance

  def appendExternalRequest(floor: Floor, direction: Direction): ElevatorSystem =
    copy(externalRequests = externalRequests + ExternalRequest(floor, direction))

  def appendInternalRequest(carId: CarId, floor: Floor): ElevatorSystem =
    copy(internalRequests = internalRequests + InternalRequest(carId, floor))

  def setCarState(carId: CarId, carState: CarState): ElevatorSystem =
    copy(elevator = elevator.copy(cars = elevator.cars + (carId -> carState)))

  def step(): Try[ElevatorSystem] = {
    val command = solver(this)
    for {
      updatedElevator <- Simulator.step(elevator, command, config.stepSize)
      updatedExternalRequests = ElevatorSystem.pruneExternalRequest(config, updatedElevator, externalRequests)
      updatedInternalRequests = ElevatorSystem.pruneInternalRequest(config, updatedElevator, internalRequests)
    } yield ElevatorSystem(config, updatedElevator, updatedExternalRequests, updatedInternalRequests, solver)
  }

  def hasExternalRequestPending(position: Distance, dir: Direction): Boolean =
    config.floorForPosition(position).exists {
      floor => externalRequests.contains(ExternalRequest(floor, dir))
    }

  def hasExternalRequestPendingAhead(position: Distance, dir: Direction): Boolean = {
    // Note external requests ahead in the opposite direction are still a motivation to continue
    // moving in the current direction
    config.floorsAhead(position, dir).exists {
      floor => externalRequests.contains(ExternalRequest(floor, dir)) ||
        externalRequests.contains(ExternalRequest(floor, dir.opposite))
    }
  }

  def hasInternalRequestPending(carId: CarId, position: Distance): Boolean =
    config.floorForPosition(position).exists {
      floor => internalRequests.contains(InternalRequest(carId, floor))
    }

  def hasInternalRequestPendingAhead(carId: CarId, position: Distance, dir: Direction): Boolean =
    config.floorsAhead(position, dir).exists {
      floor => internalRequests.contains(InternalRequest(carId, floor))
    }

  def hasOpenCarOn(floor: Floor): Boolean = {
    elevator.cars.values.exists {
      case CarStateOpen(position, _) => config.floorForPosition(position).contains(floor)
      case _ => false
    }
  }
}
