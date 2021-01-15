
package quicksignals.trackingimpl

import quicksignals.target.Target
import quicksignals.computedtarget.ComputedTarget

def tracking[A](f: Tracking ?=> A): Target[A] = new ComputedTarget[A] {
  protected def compute = {
    f(using new Tracking {
      def track[A](t: Target[A]) = relyOn(t)
    })
  }
}

trait Tracking {
  def track[A](t: Target[A]): A
}

extension[A](target: Target[A]) {
  def track(using t: Tracking): A = t.track(target)
}

