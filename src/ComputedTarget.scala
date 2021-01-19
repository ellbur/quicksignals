
package quicksignals

import collection.mutable

private trait ComputedTarget[A] extends Target[A] {
  private val listeners = mutable.ArrayBuffer[Listener]()
  private val listened = mutable.ArrayBuffer[Cancellable]()
  private var current: Option[A] = None
  
  private class Listener(upset: () => () => Unit) {
    def apply(): () => Unit = {
      upset()
    }
  }
  
  def rely(upset: () => () => Unit): (A, Cancellable) = {
    val listener = new Listener(upset)
    listeners += listener
    
    val cancellable = new Cancellable {
      def cancel(): Unit = {
        listeners -= listener
        if (listeners.isEmpty) {
          current = None
          
          val currentListened = listened.toSeq
          listened.clear()
          currentListened foreach (_.cancel())
        }
      }
    }
    
    current match {
      case Some(existing) => (existing, cancellable)
      case None =>
        val it = compute
        current = Some(it)
        (it, cancellable)
    }
  }
  
  protected def upset(): () => Unit = {
    current = None
    
    val currentListened = listened.toSeq
    listened.clear()
    currentListened foreach (_.cancel())
    
    val currentListeners = listeners.toSeq
    listeners.clear()
    val next =
      currentListeners map { l =>
        l()
      }
    () => {
      next foreach (_())
    }
  }
  
  protected def relyOn[B](s: Target[B]): B = {
    val (b, c) = s.rely(upset)
    manage(c)
    b
  }
  
  protected def manage(resource: Cancellable): Unit = {
    listened += resource
  }
  
  protected def compute: A
}

