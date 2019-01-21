package larkworthy.solver

import larkworthy.model._

object LookSolver extends Function[ElevatorSystem, ElevatorCommand] {
  override def apply(system: ElevatorSystem): ElevatorCommand = {
    ElevatorCommand(system.elevator.cars.map {
        case (carId, car) =>
          car match {
            case CarStateMoving(position, dir) =>
              if (system.hasExternalRequestPending(position, dir) ||
                system.hasInternalRequestPending(carId, position)) {
                // Check we need to open the door now to service a pending request, external or internal
                carId -> OpenDoor(dir)
              } else if (system.hasExternalRequestPendingAhead(position, dir) ||
                system.hasInternalRequestPendingAhead(carId, position, dir)) {
                // Look ahead to see if this car should keep moving because of pending request ahead
                // TODO we should look for the floor we are aiming for and slow down, we could use a large step size
                // then
                carId -> Move(system.config.moveVelocity * dir.sign)
              } else {
                carId -> SwitchDir
              }
            case CarStateOpen(_, dir) => carId -> CloseDoor(dir)
          }
    }
    )
  }
}
