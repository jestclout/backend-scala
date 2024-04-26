package game

import org.apache.pekko
import pekko.actor.typed.ActorRef
import pekko.actor.typed.Behavior
import pekko.actor.typed.Scheduler
import pekko.actor.typed.receptionist.Receptionist
import pekko.actor.typed.receptionist.ServiceKey
import pekko.actor.typed.scaladsl.ActorContext
import pekko.actor.typed.scaladsl.AskPattern._
import pekko.actor.typed.scaladsl.Behaviors
import pekko.pattern.StatusReply
import pekko.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success, Try}

object Commands extends Enumeration {
  type CommandType = Value

  val GetState = Value
  val AddPlayer = Value
  val UpdatePlayer = Value
  val RemovePlayer = Value
  val StartGame = Value
  val AnswerPrompt = Value
}

case class ManagerCmd(
    cmdType: Commands.CommandType,
    playerId: Option[Long] = None,
    player: Option[Player] = None,
    promptId: Option[Long] = None,
    answer: Option[Answer] = None
) {

  def asRunLoopCmd(
      replyTo: ActorRef[StatusReply[PublicGameState]]
  ): RunLoop.Command =
    cmdType match {
      case Commands.GetState =>
        RunLoop.GetState(playerId, replyTo)

      case Commands.AddPlayer =>
        player match {
          case Some(p) => RunLoop.AddPlayer(p, replyTo)
          case _       => throw new PlayerNotFoundException()
        }

      case Commands.UpdatePlayer =>
        player match {
          case Some(p) => RunLoop.UpdatePlayer(p, replyTo)
          case _       => throw new PlayerNotFoundException()
        }

      case Commands.RemovePlayer =>
        playerId match {
          case Some(id) => RunLoop.RemovePlayer(id, replyTo)
          case _        => throw new PlayerNotFoundException()
        }

      case Commands.StartGame =>
        RunLoop.StartGame(replyTo)

      case Commands.AnswerPrompt =>
        player match {
          case Some(p) => RunLoop.AnswerPrompt(p, replyTo)
          case _       => throw new PlayerNotFoundException()
        }
    }

}

object Manager {

  sealed trait Command
  case class CreateGame(replyTo: ActorRef[StatusReply[PublicGameState]])
      extends Command
  case class GetPublicGameState(
      code: String,
      playerId: Option[Long],
      replyTo: ActorRef[StatusReply[PublicGameState]]
  ) extends Command
  case class ExecCommand(
      code: String,
      cmd: ManagerCmd,
      replyTo: ActorRef[StatusReply[PublicGameState]]
  ) extends Command

  def randomCode =
    Random.alphanumeric.take(4).mkString.toUpperCase

  def apply(prompts: List[String]): Behavior[Command] =
    Behaviors.setup { context =>
      manager(context, Map.empty, prompts)
    }

  private def manager(
      context: ActorContext[Command],
      games: Map[String, ActorRef[RunLoop.Command]],
      prompts: List[String]
  ): Behavior[Command] = {

    implicit val ec: ExecutionContext = context.executionContext
    implicit val scheduler: Scheduler = context.system.scheduler
    implicit val timeout: Timeout = Timeout.create(
      context.system.settings.config.getDuration("jestclout.routes.ask-timeout")
    )

    def newGameCode: String = {
      val code = randomCode
      if (games.contains(code)) {
        newGameCode
      } else {
        code
      }
    }

    Behaviors.receiveMessage[Command] {
      case CreateGame(replyTo) =>
        def newGameCode: String = {
          val code = randomCode
          if (games.contains(code)) {
            newGameCode
          } else {
            code
          }
        }

        val code = newGameCode

        val runLoop = context.spawn(RunLoop(code, prompts), code)
        val newGames = games + (code -> runLoop)

        runLoop ! RunLoop.GetState(None, replyTo)

        manager(context, newGames, prompts)

      case GetPublicGameState(code, playerId, replyTo) =>
        games.get(code) match {
          case Some(game) =>
            game ! RunLoop.GetState(playerId, replyTo)

          case _ =>
            replyTo ! StatusReply.Error("game not found")
        }

        Behaviors.same

      case ExecCommand(code, cmd, replyTo) =>
        games.get(code) match {
          case Some(game) =>
            Try(cmd.asRunLoopCmd(replyTo)) match {
              case Success(runLoopCmd) =>
                game ! runLoopCmd
              case Failure(e) =>
                replyTo ! StatusReply.Error(e.getMessage)
            }

          case _ =>
            StatusReply.Error("game not found")
        }

        Behaviors.same
    }
  }
}
