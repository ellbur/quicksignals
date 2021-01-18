
import quicksignals._

object Test5 extends App {
  {
    class MutableType(init: Int) {
      var x: Int = init
      override def toString = s"[$x]"
    }
    
    val a = Source[Int](0)
    val b = Source[Int](1)
    
    val c = tracking {
      MutableType(a.track) setting (_.x = b.track)
    }

    c foreach (c => println(s"c = $c"))
    
    a() = 2
    b() = 3
  }
}

