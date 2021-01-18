
import quicksignals._
import quicksignals.tracking2impl.{tracking2, track2, setting}

object Test5 extends App {
  {
    class MutableType(init: Int) {
      var x: Int = init
      override def toString = s"[$x]"
    }
    
    val a = Source[Int](0)
    val b = Source[Int](1)
    
    val c = tracking2 {
      MutableType(a.track2) setting (_.x = b.track2)
    }

    c foreach (c => println(s"c = $c"))
    
    a() = 2
    b() = 3
  }
}

