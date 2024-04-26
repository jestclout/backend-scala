package game

import org.apache.pekko
import pekko.actor.typed.ActorRef
import pekko.actor.typed.ActorSystem
import pekko.actor.typed.scaladsl.AskPattern._
import pekko.http.scaladsl.model.StatusCodes
import pekko.http.scaladsl.server.Directive1
import pekko.http.scaladsl.server.Directives._
import pekko.http.scaladsl.server.Route
import pekko.pattern.StatusReply
import pekko.util.Timeout

import scala.concurrent.Future
import scala.util.Try

class JestCloutRoutes(gameManager: ActorRef[Manager.Command])(implicit
    val system: ActorSystem[_]
) {

  import pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  private implicit val timeout: Timeout = Timeout.create(
    system.settings.config.getDuration("jestclout.routes.ask-timeout")
  )

  def playerIdFromRequest: Directive1[Option[Long]] =
    optionalHeaderValueByName("X-Player-Id").map(_.flatMap(_.toLongOption))

  def createGame(): Future[StatusReply[PublicGameState]] =
    gameManager.ask(Manager.CreateGame.apply)

  def getState(
      code: String,
      playerId: Option[Long]
  ): Future[StatusReply[PublicGameState]] =
    gameManager.ask(Manager.GetPublicGameState(code, playerId, _))

  def execCommand(
      code: String,
      cmd: ManagerCmd
  ): Future[StatusReply[PublicGameState]] =
    gameManager.ask(Manager.ExecCommand(code, cmd, _))

  val gameRoutes: Route =
    pathPrefix("api" / "v1" / "game") {
      concat(
        pathEnd {
          post {
            onSuccess(createGame()) { status =>
              status match {
                case StatusReply.Success(response: PublicGameState) =>
                  complete((StatusCodes.Created, response))
                case _ =>
                  complete(StatusCodes.InternalServerError)
              }
            }
          }
        },
        path(Segment) { gameCode =>
          concat(
            get {
              playerIdFromRequest { playerId =>
                onSuccess(getState(gameCode, playerId)) { status =>
                  status match {
                    case StatusReply.Success(response: PublicGameState) =>
                      complete((StatusCodes.OK, response))
                    case _ =>
                      complete(StatusCodes.BadRequest)
                  }
                }
              }
            },
            post {
              optionalHeaderValueByName("X-Player-Id") { playerId =>
                entity(as[ManagerCmd]) { cmd =>
                  onSuccess(execCommand(gameCode, cmd)) { status =>
                    status match {
                      case StatusReply.Success(response: PublicGameState) =>
                        complete((StatusCodes.OK, response))
                      case _ =>
                        complete(StatusCodes.BadRequest)
                    }
                  }
                }
              }
            }
          )
        }
      )
    }
}
