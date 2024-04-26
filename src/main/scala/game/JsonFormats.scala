package game

import spray.json.{
  DefaultJsonProtocol,
  DeserializationException,
  JsNumber,
  JsValue,
  RootJsonFormat
}

object JsonFormats {
  import DefaultJsonProtocol._

  implicit val gameStatesJsonFormat: RootJsonFormat[GameStates.State] =
    new RootJsonFormat[GameStates.State] {
      def write(obj: GameStates.State) = JsNumber(obj.id)
      def read(json: JsValue) = json match {
        case JsNumber(id) => GameStates(id.toIntExact)
        case _ => throw new DeserializationException("Enumeration id expected")
      }
    }

  implicit val commandsJsonFormat: RootJsonFormat[Commands.CommandType] =
    new RootJsonFormat[Commands.CommandType] {
      def write(obj: Commands.CommandType) = JsNumber(obj.id)
      def read(json: JsValue) = json match {
        case JsNumber(id) => Commands(id.toIntExact)
        case _ => throw new DeserializationException("Enumeration id expected")
      }
    }

  implicit val playerJsonFormat: RootJsonFormat[Player] =
    jsonFormat3(Player.apply)

  implicit val promptJsonFormat: RootJsonFormat[Prompt] =
    jsonFormat2(Prompt.apply)

  implicit val answerJsonFormat: RootJsonFormat[Answer] =
    jsonFormat2(Answer.apply)

  implicit val managerCmdJsonFormat: RootJsonFormat[ManagerCmd] =
    jsonFormat5(ManagerCmd.apply)

  implicit val gameStateJsonFormat: RootJsonFormat[PublicGameState] =
    jsonFormat6(PublicGameState.apply)

}
