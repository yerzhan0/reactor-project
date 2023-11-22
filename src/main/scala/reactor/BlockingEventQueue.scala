// group 17
// 123456 Firstname Lastname
// 936323 Semin, Nikolai

package reactor

import scala.collection.mutable.Queue
import reactor.api.Event

final class BlockingEventQueue[T] (private val capacity: Int) {

  // Ensure that value for capacity is valid
  if (capacity <= 0) {
    throw new IllegalArgumentException("Capacity must be positive")
  }

  val queue = Queue[Event[T]]() // store events in a Java queue

  @throws[InterruptedException]
  def enqueue[U <: T](e: Event[U]): Unit = {
    if (e == null) { // ensure that the passed object is not null
      return
    }
    synchronized {
      while (queue.size == capacity) { // cannot enqueu while the queue is full
        wait()
        if (Thread.interrupted()) {
          throw new InterruptedException()
        }
      }
      queue += e.asInstanceOf[Event[T]] // casting required for type conformance
      notifyAll()
    }
  }

  @throws[InterruptedException]
  def dequeue: Event[T] = {
    synchronized {
      while (queue.size == 0) {
        wait()
        if (Thread.interrupted()) {
          throw new InterruptedException()
        }
      }
      val dequeuedElement = queue.dequeue()
      notifyAll()
      return dequeuedElement
    }
  }

  def getAll: Seq[Event[T]] = {
    synchronized {
      val allEvents = queue
      return allEvents.asInstanceOf[Seq[Event[T]]]
    }
  }

  def getSize: Int = {
    synchronized { // possible race condition for the queue size, hence, need to syncronize
      return queue.size
    }
  }

  // *getCapacity* can return without any concurrent mechanisms as it has no critical sections
  def getCapacity: Int = capacity

}

/* Last year feedback
Staff feedback

Not notification efficient
getAll in comments (and with no protection)
documentation not really explaining how it works
*/
