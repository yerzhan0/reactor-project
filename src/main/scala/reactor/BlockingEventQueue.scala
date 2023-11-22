// group 17
// 123456 Firstname Lastname
// 936323 Semin, Nikolai

package reactor

import java.util.concurrent.locks.{
  Condition,
  ReentrantLock
} // use ReentrantLock for better control over the lock vs synchronized or implicit locks
import scala.collection.mutable.Queue
import reactor.api.Event

final class BlockingEventQueue[T](private val capacity: Int) {

  // Ensure that value for capacity is valid
  if (capacity <= 0) {
    throw new IllegalArgumentException("Capacity must be positive")
  }

  private val queue = Queue[Event[T]]() // store events in a Java queue
  private val lock = new ReentrantLock() // lock for the queue
  private val notFull =
    lock.newCondition() // condition for the queue not being full
  private val notEmpty =
    lock.newCondition() // condition for the queue not being empty

  @throws[InterruptedException]
  def enqueue[U <: T](e: Event[U]): Unit = {
    if (e == null) { // ensure that the passed object is not null
      return
    }
    lock.lockInterruptibly() // lock the queue. May throw InterruptedException
    try {
      while (queue.size >= capacity) { // cannot enqueu while the queue is full
        notFull
          .await() // wait until the queue may be not full. May throw InterruptedException
      }
      queue += e
        .asInstanceOf[Event[T]] // casting required for type conformance
      notEmpty.signal() // signal that the queue may not be empty
    } finally {
      lock.unlock() // unlock the queue.
    }
  }

  @throws[InterruptedException]
  def dequeue: Event[T] = {
    lock.lockInterruptibly() // lock the queue. May throw InterruptedException
    try {
      while (queue.size == 0) {
        notEmpty
          .await() // wait until the queue may be not empty. May throw InterruptedException
      }
      val dequeuedElement = queue.dequeue()
      notFull.signal() // signal that the queue may not be full
      return dequeuedElement
    } finally {
      lock.unlock() // unlock the queue
    }
  }

  def getAll: Seq[Event[T]] = {
    synchronized {
      val allEvents = queue
      return allEvents.asInstanceOf[Seq[Event[T]]]
    }
  }

  def getSize: Int = {
    lock.lock() // lock the queue
    try {
      return queue.size
    } finally {
      lock.unlock() // unlock the queue
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
