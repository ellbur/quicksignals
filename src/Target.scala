
package quicksignals.target

trait Target[+A] {
  def rely(upset: () => () => Unit): (A, Cancellable)
}

trait Cancellable {
  def cancel(): Unit
}

