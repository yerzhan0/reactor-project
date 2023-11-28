// group 17
// 123456 Firstname Lastname
// 936323 Nikolai Semin

package reactor

import reactor.api.{Event, EventHandler, Handle}
import scala.collection.mutable.Set
import scala.collection.mutable.ListBuffer

final class Dispatcher(private val queueLength: Int = 10) {
  require(queueLength > 0)

  private val handlers: Set[EventHandler[Any]] = Set()
  private val eventsQueue = new BlockingEventQueue[Any](queueLength)
  private val threadList: ListBuffer[WorkerThread] = ListBuffer.empty

  /* The idea would be to start a new thread for each added handler monitoring events in there.
  Once a handle is removed the corresponding thread should be interrupted.
   */
  @throws[InterruptedException]
  def handleEvents(): Unit = {
    while (handlers.nonEmpty) {
      val event = eventsQueue.dequeue
      val handler = event.getHandler
      if (handlers.contains(handler)){
        event.dispatch()
      }
    }
  }

  def addHandler[T](h: EventHandler[T]): Unit = {
    val thread = new WorkerThread(h.asInstanceOf[EventHandler[Any]])
    threadList += thread
    handlers += h.asInstanceOf[EventHandler[Any]]
    thread.start()
  }

  def removeHandler[T](h: EventHandler[T]): Unit = {
    val removed = handlers.remove(h.asInstanceOf[EventHandler[Any]])
    if (removed) {
      threadList.foreach { thread =>
        if (thread.getHandler == h) {
          thread.cancelThread()
        }
      }
    }
  }

  // Hint:
  final class WorkerThread(private val handler: EventHandler[Any])
      extends Thread {
    def getHandler: EventHandler[Any] = handler
    override def run(): Unit = {
      val handle = handler.getHandle
      var eventData = handle.read
      while (eventData != null) {
        eventsQueue.enqueue(Event(eventData, handler))
        eventData = handle.read
      }
      eventsQueue.enqueue(Event(eventData, handler))
    }

    def cancelThread(): Unit = {
      this.interrupt()
    }
  }
}
/*
// Hint:
final class WorkerThread[T](???) extends Thread {

 override def run(): Unit = ???

 def cancelThread(): Unit = ???

}
 */
