
package quicksignals.trackingimpl

import quicksignals.target.Target
import quicksignals.computedmutableupdate.ComputedMutableUpdate

def tracking[A](f: Tracking ?=> A): Target[A] = new ComputedMutableUpdate[A] {
  protected def compute = {
    f(using new Tracking {
      def track[A](t: Target[A]) = relyOn(t)
      def setting(u: Target[() => Unit]) = relyOnUpdater(u)
    })
  }
}

trait Tracking {
  def track[A](t: Target[A]): A
  def setting(u: Target[() => Unit]): Unit
}

extension[A](target: Target[A]) {
  def track(using t: Tracking): A = t.track(target)
}

extension[A](x: A) {
  def setting(f: Tracking ?=> A => Unit)(using t: Tracking): x.type = {
    t.setting(tracking {
      () => f(x)
    })
    x
  }
}

