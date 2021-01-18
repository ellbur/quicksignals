
package quicksignals.computedmutableupdate

import scala.collection.mutable
import quicksignals.target.{Target, Cancellable}
import quicksignals.trackingimpl.{tracking, Tracking}
import quicksignals.monadops.map

type Mutator[-A] = A => Unit

trait ComputedMutableUpdate[A] extends Target[A] {
  private val listeners = mutable.ArrayBuffer[Listener]()
  private val listened = mutable.ArrayBuffer[Cancellable]()
  private val listenedUpdater = mutable.ArrayBuffer[Cancellable]()
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
          val currentListenedUpdater = listenedUpdater.toSeq
          
          listened.clear()
          listenedUpdater.clear()
          
          currentListened foreach (_.cancel())
          currentListenedUpdater foreach (_.cancel())
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
  
  private def upset(): () => Unit = {
    current = None
    
    val currentListened = listened.toSeq
    val currentListenedUpdater = listenedUpdater.toSeq
    
    listened.clear()
    listenedUpdater.clear()
    
    currentListened foreach (_.cancel())
    currentListenedUpdater foreach (_.cancel())
    
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
  
  private def upsetUpdater(updateTarget: Target[() => Unit], cancel: => Cancellable): () => () => Unit = {
    () => {
      listenedUpdater -= cancel
      () => {
        relyOnUpdater(updateTarget)
      }
    }
  }
  
  protected def relyOn[B](s: Target[B]): B = {
    val (b, c) = s.rely(upset)
    listened += c
    b
  }
  
  protected def relyOnUpdater(updateTarget: Target[() => Unit]): Unit = {
    lazy val reliance = updateTarget.rely(upsetUpdater(updateTarget, cancel))
    lazy val updater: () => Unit = reliance._1
    lazy val cancel: Cancellable = reliance._2
    
    listenedUpdater += cancel
    
    updater()
  }
  
  protected def compute: A
}

