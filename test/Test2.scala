
import quicksignals._
import quicksignals.tracking2impl.{tracking2, track2, setting}

object Test2 extends App {
  {
    class MutableType(initX: Int) {
      println(s"> Iniatializing with $initX")
      private var x: Int = initX
      def setX(next: Int): Unit = {
        println(s" * Setting to $next")
        x = next
      }
      override def toString = s"[$x]"
    }
    
    val s = Source[MutableType](MutableType(0))
    val r = Source[MutableType => Unit](_.setX(1))
    
    val sp = tracking {
      println("(calc s)")
      s.track
    }
    
    val rp = tracking {
      println("(calc r)")
      r.track
    }
    
    val a = tracking2 {
      sp.track2 setting rp.track2
    }
    
    val cancelForeach = a foreach println
    
    r() = (_.setX(2))
    r() = (_.setX(3))
    
    s() = MutableType(4)
    
    println("== Cancelling foreach ==")
    cancelForeach.cancel()
    
    r() = (_.setX(5))
    r() = (_.setX(6))
    
    println("== Restarting foreach ==")
    val cancelForeach2 = a foreach println
  }
}

