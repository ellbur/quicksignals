
package quicksignals.nowimpl
import quicksignals.target.Target

extension[A](target: Target[A]) {
  def now: A = {
    val (a, c) = target.rely(() => () => ())
    c.cancel()
    a
  }
}

