
package quicksignals.tracking2impl

import quicksignals.target.Target
import quicksignals.computedmutableupdate2.ComputedMutableUpdate2

def tracking2[A](f: Tracking2 ?=> A): Target[A] = new ComputedMutableUpdate2[A] {
  protected def compute = {
    f(using new Tracking2 {
      def track[A](t: Target[A]) = relyOn(t)
      def setting(u: Target[() => Unit]) = relyOnUpdater(u)
    })
  }
}

trait Tracking2 {
  def track[A](t: Target[A]): A
  def setting(u: Target[() => Unit]): Unit
}

extension[A](target: Target[A]) {
  def track2(using t: Tracking2): A = t.track(target)
}

extension[A](x: A) {
  def setting(f: Tracking2 ?=> A => Unit)(using t: Tracking2): x.type = {
    t.setting(tracking2 {
      () => f(x)
    })
    x
  }
}

