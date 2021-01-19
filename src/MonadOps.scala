
package quicksignals

extension[A, B](target: Target[A]) {
  def map(f: A => B): Target[B] = tracking { f(target.track) }
  def flatMap(f: A => Target[B]) = tracking { f(target.track).track }
  def zip(other: Target[B]): Target[(A, B)] = tracking { (target.track, other.track) }
}

