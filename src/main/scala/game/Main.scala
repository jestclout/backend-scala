package game

import org.apache.pekko
import pekko.actor.typed.ActorSystem
import pekko.actor.typed.scaladsl.Behaviors
import pekko.http.scaladsl.Http
import pekko.http.scaladsl.server.Route

import scala.io.Source
import scala.util.Failure
import scala.util.Success

object Server {

  private def startHttpServer(
      routes: Route
  )(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 3001).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "Server online at http://{}:{}/",
          address.getHostString,
          address.getPort
        )

      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {

    val prompts = Source.fromResource("prompts.txt").getLines.toList

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val managerActor = context.spawn(Manager(prompts), "ManagerActor")
      context.watch(managerActor)

      val routes = new JestCloutRoutes(managerActor)(context.system)
      startHttpServer(routes.gameRoutes)(context.system)

      Behaviors.empty
    }

    val system = ActorSystem[Nothing](rootBehavior, "JestCloutPekkoHttpServer")

  }
}
