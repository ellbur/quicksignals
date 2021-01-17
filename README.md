
# Introduction

`quicksignals` is a proof of concept to show just how tiny a [functional-reactive programming (FRP)](https://infoscience.epfl.ch/record/148043) library can be and still be useful. 

`quicksignals` does not aim to provide any better or different functionality than the multitude of existing FRP libraries. Rather, it aims to be small enough to make it easy to understand its design.

`quicksignals` is written in Scala 3. 

# The `Target` protocol

## Definition

The `Target` protocol defines a "signal" in the `quicksignals` universe. (The name "target" is chosen by analogy to makefiles.)

A target is something that can be computed, that will invoke a callback when the computed value is no longer valid, and which can have the wait for the callback cancelled if updated values are no longer needed:

```Scala
trait Target[+A] {
  def rely(upset: () => () => Unit): (A, Cancellable)
}

trait Cancellable {
  def cancel(): Unit
}
```

The callback has type `() => () => Unit` because there are two passes: the first to invalidate the current value, and the second to do any follow-up computations.

It turns out this minimal protocol is more powerful than it first appears.

## Characteristics

### Synchronous

The `Target` protocol is entirely synchronous. A consequence is that `Target`s always appear to be in a valid state. Put another way, there are no "dynamic hazards" as occur in asynchronous FRP protocols.

Consider the following example:

```Scala
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
```

This is the infamous diamond dependency:

![base and ext depend on name, `baseAndExt` depends on base and ext](https://d35yeutfwbbcir.cloudfront.net/hosting/2021-01-16/ybx3nb3ol1/diamong-1.png)

`(None, Some(".txt"))` is an invalid state for `baseAndExt`. It can arise only if the computation for `base` happens before the computation for `ext`:

![computation reaches base, then `baseAndExt`, before ext updates](https://d35yeutfwbbcir.cloudfront.net/hosting/2021-01-16/z5xpsu3p8k/diamong-2.png)

However, the `Target` protocol is synchronous, so the invalid value never arises. You can treat *two* signals as if they were *one* signal: changes will always be atomic. 

In asynchronous systems, dealing with temporary inconsistencies is inevitable. Some systems have to be asynchronous. In distributed systems, for example, efficiency demands that you not wait for all changes to propagate before acting.

But GUIs are mostly synchronous. In fact, your brain is already trained to think of GUIs as synchronous. When you type in a text editor, you don't wait for one key to complete before hitting the next key. You know that the keys will come out in the order you type them and won't race to get inserted into the document. So there is a place for synchronous signals. 

### Composable

If you are used to GUI programming, you will recognize the synchronous behavior of the `Target` protocol matching the behavior of an event loop. (If you've used Verilog or VHDL, you will recognize it matches their simulation models, which also resemble an event loop.) You know that when you modifier widget properties in an event handler, the GUI doesn't redraw on every call to `setText()` and `setBackground()`: you get one redraw with all the new values on the next paint event. That means that event-loop-based code avoids inconsistencies just like the `Target` protocol. 

The problem with event loops, though, is that they are like global variables: your program has one event loop, and any code that emits events must refer back to it. The result is that event-loop code is harder to build in pieces: you need a common scheme to manage the event loop before you can string together the parts that use it. 

The `Target` protocol is like an emergent event loop. You make your `Target`s independently, and when you combine them into a single program, they automatically synchronize. The result is that the `Target` protocol achieves that functional-programming buzzword: "composable".

### Stative

I don't know a good word for this aspect of the `Target` protocol, so I'm calling it "stative", to distinguish it from "active" FRP signals.

An "active" FRP signal is a stream of changes. The "current" value of the signal is the value of the most recent change. 

The "stream of changes" model does not work for the `Target` protocol. In fact, any FRP system that avoids the diamond problem can't be modeled as a stream of changes because you can have fewer "output" changes than "input" changes:

![changes at the mid-diamond become one change at the end](https://d35yeutfwbbcir.cloudfront.net/hosting/2021-01-16/dlnpr870oi/diamond-3.png)

This creates the odd situation that a `Target` can model the current state of the signal but not the transitions between states.

# Parts

## `ComputedTarget`

The real logic of the library takes place in [`ComputedTarget`](https://github.com/ellbur/quicksignals/blob/main/src/quicksignals/ComputedTarget.scala), which handles the fussy details of invoking callbacks and cancelling dependencies. There's nothing magical or enlightening about code. It turns out the rules for updating invalid values are just boring and finicky.

But once those are written, everything can be based off of it. 

## `Source`

When you have a protocol, you need a way to get data into it. That's where `Source` comes in, which is is a `Target` that can be set using ordinary imperative code:

```Scala
val s = Source[Int](0)
s() = 1
```

## `tracking`

Once you have sources, you need a way to consume their values. For that, there is `tracking`:

```Scala
val s = Source[Int](0)
val r = Source[Int](1)

val a = tracking {
  s.track + r.track
}
```

## Monad operators

Monad operators can be implemented on top of `tracking`:

```Scala
extension[A, B](target: Target[A]) {
  def map(f: A => B): Target[B] = tracking { f(target.track) }
  def flatMap(f: A => Target[B]) = tracking { f(target.track).track }
  def zip(other: Target[B]): Target[(A, B)] = tracking { (target.track, other.track) }
}
```

## Sinks part 1: `foreach`

You can't tell what values are in your signals unless the signals can produce side-effects outside of the library. This is an awkward area of every FRP library, mostly because it involves resource management. One of the goals of `quicksignals` is to explore ways to make the boundaries of the library smoother while keeping the logic succinct.

Still, to get a hold of the problem, it's worth starting off with the simple yet surprisingly bad `foreach`:

```Scala
extension[A](target: Target[A]) {
  def foreach(f: A => Unit): Cancellable = {
    var workingCancellable: Option[Cancellable] = None
    def go(): Unit = {
      val (x, canc) = target.rely { () =>
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
```

Which can be used as follows:

```Scala
val s = Source[Int](0)
val cancelForeach = s foreach println
s() = 1
s() = 2
cancelForeach.cancel()
s() = 3
```

It should be immediately obvious that this is bad, because it's setting up a resource leak. And in a language like Scala where you are not used to managing resources, it's easy to forget those few times when you have to.

A common technique from FRP libraries is to use weak references and stop the `foreach` when the last consumer is garbage collected. That's OK, but it's not great. First, because objects can hang around a long time before the JVM garbage collector runs, and, second, because once you start using weak references it's easy to accidentally create additional strong references and thwart the whole system. And, of course, you can't use this strategy in JavaScript, which still, somehow, does not have weak references. 

Still, `foreach` is a start. Next it is time to see where we can go from here. 

