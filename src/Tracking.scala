
package quicksignals

def tracking[A](f: Tracking ?=> A): Target[A] = new ComputedTarget[A] {
  protected def compute = {
    f(using new Tracking {
      def track[A](t: Target[A]) = relyOn(t)
      def setting(u: Target[() => Unit]) = manage(u foreach (_()))
    })
  }
}

trait Tracking {
  private[quicksignals] def track[A](t: Target[A]): A
  private[quicksignals] def setting(u: Target[() => Unit]): Unit
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

