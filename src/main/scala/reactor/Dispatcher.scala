// group 1
// 123456 Firstname Lastname
// 936323 Nikolai Semin

package reactor

import reactor.api.{Event, EventHandler}

final class Dispatcher(private val queueLength: Int = 10) {
  require(queueLength > 0)

  private val queue = new BlockingEventQueue[Event[_]](queueLength)

  @throws[InterruptedException]
  def handleEvents(): Unit = {
      val mainHandlindThread = new WorkerThread
      mainHandlindThread.run()
  }

  def addHandler[T](h: EventHandler[T]): Unit = {

  }

  def removeHandler[T](h: EventHandler[T]): Unit = {
    
  }

}


// Hint:
final class WorkerThread[T]() extends Thread {

 override def run(): Unit = {
  // do events habdling in that thread
 }

 def cancelThread(): Unit = ???

}
