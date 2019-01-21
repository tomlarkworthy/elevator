package larkworthy.model

import org.scalacheck.{Prop, Properties}
import org.scalacheck.Prop.forAll

/**
  * The main test for the work, we used randomized
  */
object ElevatorSystemSpec extends Properties("ElevatorSystem") {

  property("nonperiodic") = nonPeriodic

  /**
    * All elevator systems eventually serve all requests, they never get stuck in an infinite loop, progress is
    * monotonic
    */
  def nonPeriodic: Prop = forAll(Generators.genElevatorSystem) {
    system: ElevatorSystem => {
      var history = Set[ElevatorSystem]()
      var current = system
      while (current.externalRequests.nonEmpty
        || current.internalRequests.nonEmpty
        && !history.contains(current)) {
        history = history + current
        current = current.step().get
      }
      current.externalRequests.isEmpty && current.internalRequests.isEmpty
    }
  }
}
