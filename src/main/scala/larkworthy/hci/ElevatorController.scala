package larkworthy.hci

import larkworthy.model.ElevatorSystem.{CarId, Floor}
import larkworthy.model._

/**
  * Top level solution to the Task.
  * @param config static elevator configuration
  * @param state initial state of the elevator
  */
class ElevatorController(val config: ElevatorConfig,
                         var state: ElevatorSystem) {
  /**
    * The state of the Elevator.
    * @see ElevatorState
    */
  def status(): ElevatorState = state.elevator

  /**
    * Pressed by people waiting on a floor
    * @param floor which floor the people are on
    * @param direction which direction is requested
    */
  def callElevator(floor: Floor, direction: Direction): Unit =
    state = state.appendExternalRequest(floor, direction)

  /**
    * Pressed by people within an elevator, requesting a floor to be taken to
    * @param carId the car the person is currently in
    * @param destination the floor the person would like the elevator to open on
    */
  def requestDropoff(carId: CarId, destination: Floor): Unit =
    state = state.appendInternalRequest(carId, destination)

  /**
    * Sets the current state of a Car, for example, this could be driven by the physical world instead
    * of using the step
    * @param carId
    * @param carState
    */
  def update(carId: CarId, carState: CarState): Unit = {
    state = state.setCarState(carId, carState)
  }
  /**
    * Steps the simulation a time unit.
    * @see ElevatorConfig#stepSize()
    */
  def step(): Unit = state = state.step().get
}
