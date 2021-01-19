
package quicksignals

class Source[A](private var it: A) extends ComputedTarget[A] {
  def update(next: A): Unit = {
    it = next
    upset()()
  }
  
  protected def compute: A = it
}

// https://github.com/lampepfl/dotty/issues/11128
object Source

