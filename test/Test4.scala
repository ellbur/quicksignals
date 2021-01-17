
import quicksignals._

object Test4 extends App {
  {
    val name = Source[Option[String]](Some("file.txt"))
    val pattern = "(.*)\\.(.*)".r
    
    val base = name map {
      case Some(`pattern`(a, _)) => a
      case _ => None
    }
    
    val ext = name map {
      case Some(`pattern`(_, b)) => b
      case _ => None
    }
    
    val baseAndExt = base zip ext
    
    baseAndExt foreach println
    
    name() = None
    name() = Some("xyz")
    name() = Some("abc.xyz")
    name() = None
  }
}

