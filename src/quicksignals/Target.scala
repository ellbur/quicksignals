
package quicksignals

import scala.collection.mutable

trait Target[+A] {
  def rely(upset: () => () => Unit): (A, Cancellable)
  
  import TrackingUtils.tracking
  
  def map[B](f: A => B): Target[B] = tracking { f(this.track) }
  def flatMap[B](f: A => Target[B]) = tracking { f(this.track).track }
  def zip[B](other: Target[B]): Target[(A, B)] = tracking { (this.track, other.track) }
  
  def track(using tracking: Tracking): A = tracking.track(this)
  
  def foreach(f: A => Unit): Cancellable = {
    var workingCancellable: Option[Cancellable] = None
    def go(): Unit = {
      val (x, canc) = rely { () =>
        workingCancellable = None
        { () =>
          go()
        }
      }
      workingCancellable = Some(canc)
      f(x)
    }
    go()
    new Cancellable {
      def cancel(): Unit = {
        workingCancellable foreach (_.cancel())
      }
    }
  }
}

trait Cancellable {
  def cancel(): Unit
}

trait ComputedTarget[A] extends Target[A] {
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
    listened += c
    b
  }
  
  protected def compute: A
}

trait Mutator[-A] {
  def mutate(x: A): Unit
}

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
            mutator.mutate(primary)
            (primary, cancellable)
          
          case None =>
            val mutator = relyOnUpdater(updater)
            currentMutator = Some(mutator)
            mutator.mutate(primary)
            (primary, cancellable)
        }
        
      case None =>
        val (primary, updater) = relyOn(child)
        current = Some((primary, updater))
        
        val mutator = relyOnUpdater(updater)
        currentMutator = Some(mutator)
        mutator.mutate(primary)
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
        mutator.mutate(primary)
      }
    }
  }
  
  protected def relyOnUpdater[B](s: Target[B]): B = {
    val (b, c) = s.rely(upsetUpdater)
    listenedUpdater += c
    b
  }
}

class Source[A](_init: => A) extends ComputedTarget[A] {
  private var it: A = _init
  
  def update(next: A): Unit = {
    it = next
    upset()()
  }
  
  protected def compute: A = it
}

object TrackingUtils {
  def tracking[A](f: Tracking ?=> A): Target[A] = new ComputedTarget[A] {
    protected def compute = {
      f(using new Tracking {
        def track[A](t: Target[A]) = relyOn(t)
      })
    }
  }
}

trait Tracking {
  def track[A](t: Target[A]): A
}

