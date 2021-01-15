
import quicksignals._

object Test3 extends App {
  {
    val s = Source[Int](0)
    val r = Source[Int](1)
    
    val a = tracking {
      println("(calc outer)")
      (s.track, tracking {
        println("(calc inner)")
        r.track
      })
    }
    
    a foreach println
    
    r() = 2
    r() = 3
  }
}


