
package quicksignals.computedmutableupdate

import scala.collection.mutable
import quicksignals.target.{Target, Cancellable}
import quicksignals.trackingimpl.{tracking, Tracking}
import quicksignals.monadops.map

extension[A](target: Target[A]) {
  def setting(f: Tracking ?=> A => Unit): Target[A] = ComputedMutableUpdate[A](
    target map ((_, tracking(f)))
  )
}

type Mutator[-A] = A => Unit

class ComputedMutableUpdate[A](child: Target[(A, Target[Mutator[A]])]) extends Target[A] {
  private val listeners = mutable.ArrayBuffer[Listener]()
  private val listened = mutable.ArrayBuffer[Cancellable]()
  private val listenedUpdater = mutable.ArrayBuffer[Cancellable]()
  private var current: Option[(A, Target[Mutator[A]])] = None
  private var currentMutator: Option[Mutator[A]] = None
  
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
          currentMutator = None
          
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
      case Some((primary, updater)) =>
        currentMutator match {
          case Some(mutator) =>
            mutator(primary)
            (primary, cancellable)
          
          case None =>
            val mutator = relyOnUpdater(updater)
            currentMutator = Some(mutator)
            mutator(primary)
            (primary, cancellable)
        }
        
      case None =>
        val (primary, updater) = relyOn(child)
        current = Some((primary, updater))
        
        val mutator = relyOnUpdater(updater)
        currentMutator = Some(mutator)
        mutator(primary)
        (primary, cancellable)
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
    listened += c
    b
  }
  
  protected def upsetUpdater(): () => Unit = {
    currentMutator = None
    
    val currentListenedUpdater = listenedUpdater.toSeq
    listenedUpdater.clear()
    currentListenedUpdater foreach (_.cancel())
    
    () => {
      current foreach { case (primary, updater) =>
        val mutator = relyOnUpdater(updater)
        currentMutator = Some(mutator)
        mutator(primary)
      }
    }
  }
  
  protected def relyOnUpdater[B](s: Target[B]): B = {
    val (b, c) = s.rely(upsetUpdater)
    listenedUpdater += c
    b
  }
}

