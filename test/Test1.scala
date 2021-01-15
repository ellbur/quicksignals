
import quicksignals._

object Test1 extends App {
  {
    val s = new Source[Int](0)
    val a = tracking {
      println("Calculating a")
      s.track + 1
    }
    val b = tracking {
      println("Calculating b")
      a.track + 2
    }
    val c = tracking {
      println("Calculating c")
      a.track + 4
    }
    val d = tracking {
      println("Calculating d")
      b.track + c.track
    }
    
    val (d1, canc1) = d.rely { () =>
      println("Upset 1")
      { () =>
        println("Upset 2")
      }
    }
    println(s"d1 = $d1")
    
    s() = 8
    s() = 16
    
    val (d2, canc2) = d.rely { () =>
      println("Upset 3")
      { () =>
        println("Upset 4")
      }
    }
    println(s"d2 = $d2")
  }
}

