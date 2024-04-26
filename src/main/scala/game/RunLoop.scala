package game

import org.apache.pekko
import pekko.actor.typed.ActorRef
import pekko.actor.typed.Behavior
import pekko.actor.typed.scaladsl.ActorContext
import pekko.actor.typed.scaladsl.Behaviors
import pekko.pattern.StatusReply

import scala.util.{Failure, Success, Try}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object RunLoop {

  sealed trait Command

  final case class GetState(
      playerId: Option[Long],
      replyTo: ActorRef[StatusReply[PublicGameState]]
  ) extends Command

  final case class AddPlayer(
      player: Player,
      replyTo: ActorRef[StatusReply[PublicGameState]]
  ) extends Command

  final case class UpdatePlayer(
      player: Player,
      replyTo: ActorRef[StatusReply[PublicGameState]]
  ) extends Command

  final case class RemovePlayer(
      playerId: Long,
      replyTo: ActorRef[StatusReply[PublicGameState]]
  ) extends Command

  final case class StartGame(replyTo: ActorRef[StatusReply[PublicGameState]])
      extends Command

  final case class AnswerPrompt(
      player: Player,
      replyTo: ActorRef[StatusReply[PublicGameState]]
  ) extends Command

  final case object AnsweringPromptsTimeout extends Command
  final case object VotingOnAnswersTimeout extends Command
  final case object TimeoutFailed extends Command

  def apply(code: String, prompts: List[String]): Behavior[Command] =
    Behaviors.setup { context =>
      playersRunLoop(context, Instance(code, prompts))
    }

  private def playersRunLoop(
      context: ActorContext[Command],
      instance: Instance
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetState(playerId, replyTo) =>
        val publicState = instance.getState(playerId)
        replyTo ! StatusReply.Success(publicState)
        Behaviors.same

      case AddPlayer(player, replyTo) =>
        Try(instance.addPlayer(player)) match {
          case Success(newInstance) =>
            val publicState = newInstance.getState(player.id)
            replyTo ! StatusReply.Success(publicState)

            playersRunLoop(context, newInstance)

          case Failure(e) =>
            replyTo ! StatusReply.Error(e.getMessage)

            Behaviors.same
        }

      case UpdatePlayer(player, replyTo) =>
        Try(instance.updatePlayer(player)) match {
          case Success(newInstance) =>
            val publicState = newInstance.getState(player.id)
            replyTo ! StatusReply.Success(publicState)

            playersRunLoop(context, newInstance)

          case Failure(e) =>
            replyTo ! StatusReply.Error(e.getMessage)

            Behaviors.same
        }

      case RemovePlayer(playerId, replyTo) =>
        Try(instance.removePlayer(playerId)) match {
          case Success(newInstance) =>
            val publicState = newInstance.getState()
            replyTo ! StatusReply.Success(publicState)

            playersRunLoop(context, newInstance)

          case Failure(e) =>
            replyTo ! StatusReply.Error(e.getMessage)

            Behaviors.same
        }

      case StartGame(replyTo) =>
        val newInstance = instance.advanceState
        val publicState = newInstance.getState()
        replyTo ! StatusReply.Success(publicState)

        implicit val ec: ExecutionContext = context.executionContext

        val timeout = Future(Thread.sleep(6.seconds.toMillis))
        context.pipeToSelf(timeout) {
          case Success(_) => AnsweringPromptsTimeout
          case Failure(_) => TimeoutFailed
        }

        answeringRunLoop(context, newInstance)

      case _ =>
        Behaviors.same
    }

  private def answeringRunLoop(
      context: ActorContext[Command],
      instance: Instance
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetState(playerId, replyTo) =>
        val publicState = instance.getState(playerId)
        replyTo ! StatusReply.Success(publicState)
        Behaviors.same

      case AnsweringPromptsTimeout =>
        val newInstance = instance.advanceState
        votingRunLoop(context, newInstance)

      case _ =>
        Behaviors.same
    }

  private def votingRunLoop(
      context: ActorContext[Command],
      instance: Instance
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetState(playerId, replyTo) =>
        val publicState = instance.getState(playerId)
        replyTo ! StatusReply.Success(publicState)
        Behaviors.same

      case _ =>
        Behaviors.same
    }

  private def scoringRunLoop(
      context: ActorContext[Command],
      instance: Instance
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetState(playerId, replyTo) =>
        val publicState = instance.getState(playerId)
        replyTo ! StatusReply.Success(publicState)
        Behaviors.same

      case _ =>
        Behaviors.same
    }

  private def endGameRunLoop(
      context: ActorContext[Command],
      instance: Instance
  ): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetState(playerId, replyTo) =>
        val publicState = instance.getState(playerId)
        replyTo ! StatusReply.Success(publicState)
        Behaviors.same

      case _ =>
        Behaviors.same
    }
}
