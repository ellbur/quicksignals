
package quicksignals

extension[A](target: Target[A]) {
  def foreach(f: A => Unit): Cancellable = {
    var workingCancellable: Option[Cancellable] = None
    def go(): Unit = {
      val (x, canc) = target.rely { () =>
        workingCancellable = None
        { () =>
          go()
        }
      }
      workingCancellable = Some(canc)
      f(x)
    }
    go()
    new Cancellable {
      def cancel(): Unit = {
        workingCancellable foreach (_.cancel())
      }
    }
  }
}

