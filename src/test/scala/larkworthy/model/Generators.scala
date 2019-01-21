package larkworthy.model

import larkworthy.model.ElevatorSystem.{ExternalRequest, InternalRequest}
import larkworthy.solver.LookSolver
import org.scalacheck.Gen
import squants.motion.Distance
import squants.space.Meters


object Generators {

  def genDistance: Gen[Distance] = Gen.choose(-20.0, 20.0).map{
    double => Simulator.roundToMillis(Meters(double))
  }
  def genDirection: Gen[Direction] = Gen.oneOf(DOWN, UP)

  def genConfig: Gen[ElevatorConfig] = for {
    floorsN <- Gen.choose(1, 5)
    distances <- Gen.listOfN(floorsN, genDistance).suchThat(
      // Bug Fix: We need to ensure floors don't unrealistically overlap as it can cause problems
      // when a car stops in the overlapping zone, its uncertain which floor the car is on
      distances => distances.forall(
        distance => !distances.exists(
          otherDistance => otherDistance != distance && (distance - otherDistance).abs < Meters(0.5)
        )
      )
    )
    floorPositions = distances.zipWithIndex.map {
      case (distance, index) => index -> distance
    }.toMap
  } yield ElevatorConfig(floorPositions)

  def genMovingCar: Gen[CarStateMoving] = for {
    position <- genDistance
    dir <- genDirection
  } yield CarStateMoving(position, dir)

  def genOpenCar: Gen[CarStateOpen] = for {
    position <- genDistance
    dir <- genDirection
  } yield CarStateOpen(position, dir)

  def genCar: Gen[CarState] = Gen.oneOf(genMovingCar, genOpenCar)

  def genElevator(elevatorSystemConfig: ElevatorConfig): Gen[ElevatorState] = for {
    carN <- Gen.choose(1, 16)
    carStates <- Gen.listOfN(carN, genCar)
    cars = carStates.zipWithIndex.map {
      case (car, index) => index -> car
    }.toMap
  } yield ElevatorState(cars)

  def genExternalRequest(config: ElevatorConfig): Gen[ExternalRequest] = for {
    floor <- Gen.oneOf(config.floorPositions.keys.toList)
    direction <- genDirection
  } yield ExternalRequest(floor, direction)

  def genInternalRequest(config: ElevatorConfig, elevator: ElevatorState): Gen[InternalRequest] = for {
    floor <- Gen.oneOf(config.floorPositions.keys.toList)
    carId <- Gen.oneOf(elevator.cars.keys.toList)
  } yield InternalRequest(carId, floor)

  def genElevatorSystem: Gen[ElevatorSystem] = for {
    config <- genConfig
    elevator <- genElevator(config)
    externalRequests <- Gen.containerOf[Set, ExternalRequest](genExternalRequest(config))
    internalRequests <- Gen.containerOf[Set, InternalRequest](genInternalRequest(config, elevator))

  } yield ElevatorSystem(config, elevator, externalRequests, internalRequests, LookSolver)
}
