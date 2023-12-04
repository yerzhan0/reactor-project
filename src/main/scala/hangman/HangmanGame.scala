// group 1
// 123456 Firstname Lastname
// 654321 Firstname Lastname

package hangman

import reactor.api.{EventHandler}
import reactor.Dispatcher
import hangman.util.TCPTextHandle
import hangman.util.AcceptHandle
import java.net.Socket

class HangmanGame(val hiddenWord: String, val initialGuessCount: Int) {
  require(hiddenWord != null && hiddenWord.length > 0)
  require(initialGuessCount > 0)

  private var players = Set.empty[Player]
  var connectionHandler = Option.empty[HangmanConnectionHandler]
  val dispatcher = new Dispatcher(10)
  var gameState = new GameState(hiddenWord, initialGuessCount, Set.empty[Char])

  // Public func to start the run loop
  def start() {
    this.connectionHandler = Option(
      new HangmanConnectionHandler(new AcceptHandle(Option.empty))
    )
    dispatcher.addHandler(connectionHandler.get)
    try {
      dispatcher.handleEvents()
    } catch {
      case e: InterruptedException => {
        println("Terminating game...")
        terminate()
      }
    }
  }

  def terminate() {
    players.foreach(player => {
      player.getHandle.close
    })
    connectionHandler.get.getHandle.close
    dispatcher.removeHandler(connectionHandler.get)
    println("Game terminated")
  }

  final class HangmanConnectionHandler(val handle: AcceptHandle)
      extends EventHandler[Socket] {
    override def getHandle: AcceptHandle = handle

    override def handleEvent(socket: Socket): Unit = {
      val tcpHandle = new TCPTextHandle(socket)
      val messageHandler = new HangmanMessageHandler(tcpHandle)
      dispatcher.addHandler(messageHandler)
    }
  }

  final class HangmanMessageHandler(val handle: TCPTextHandle)
      extends EventHandler[String] {
    var player = Option.empty[Player]

    override def getHandle: TCPTextHandle = handle

    override def handleEvent(message: String): Unit = {
      if (message == null) {
        println("Player " + player.get.getName + " disconnected")
        dispatcher.removeHandler(this)
        players = players - player.get
        return
      }
      player match {
        case None    => handleNewPlayer(message)
        case Some(_) => handlePlayerMessage(message)
      }
    }

    private def handleNewPlayer(message: String): Unit = {
      if (message == null) return
      val newPlayer = new Player(message, handle)
      newPlayer.send(gameState.getMaskedWord + " " + gameState.guessCount)
      players = players + newPlayer
      this.player = Option(newPlayer)
    }

    private def handlePlayerMessage(message: String): Unit = {
      if (message == null) return
      val guess = message.charAt(0)
      gameState = gameState.makeGuess(guess)
      players.foreach(player =>
        player.send(
          guess + " " + gameState.getMaskedWord + " " + gameState.guessCount + " " + this.player.get.getName
        )
      )
      if (gameState.isGameOver) {
        terminate()
      }
    }

  }

  final class Player(
      private val name: String,
      private val handle: TCPTextHandle
  ) {
    def send(message: String): Unit = {
      handle.write(message)
    }
    def getName: String = name
    def getHandle: TCPTextHandle = handle
  }
}

object HangmanGame {

  def main(args: Array[String]): Unit = {
    val word: String = args(0) // first program argument
    val guessCount: Int = args(1).toInt // second program argument

    val game: HangmanGame = new HangmanGame(word, guessCount)
    // TODO start the game
    game.start()
  }

}
