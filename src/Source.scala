
package quicksignals.source

import quicksignals.computedtarget.ComputedTarget

class Source[A](_init: => A) extends ComputedTarget[A] {
  private var it: A = _init
  
  def update(next: A): Unit = {
    it = next
    upset()()
  }
  
  protected def compute: A = it
}

// https://github.com/lampepfl/dotty/issues/11128
object Source

