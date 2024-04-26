package game

case class Player(
    id: Option[Long],
    name: String,
    score: Option[Int]
)

object GameStates extends Enumeration {
  type State = Value

  val WaitingForPlayers = Value
  val AnsweringPrompts = Value
  val VotingOnAnswers = Value
  val ScoringRound = Value
  val ScoringGame = Value
  val Done = Value
}

case class Prompt(
    id: Long,
    text: String
)

case class Answer(
    id: Long,
    text: String
)

case class PublicGameState(
    code: String,
    players: List[Player],
    currentState: GameStates.State,
    prompt: Option[Prompt] = None,
    answers: List[Answer] = List.empty,
    votes: Map[Long, Long] = Map.empty
)

case class UserAnswer(
    playerId: Long,
    prompt: Prompt,
    answer: Option[Answer] = None
)

case class Round(
    prompts: List[Prompt],
    userAnswers: Map[Long, List[UserAnswer]]
)

case class Instance(
    code: String,
    possiblePrompts: List[String],
    players: List[Player] = List.empty,
    currentState: GameStates.State = GameStates.WaitingForPlayers,
    currentRound: Int = 0,
    currentVotingPrompt: Int = 0,
    nextPlayerID: Long = 1,
    nextPromptID: Long = 1,
    nextAnswerID: Long = 1,
    rounds: List[Round] = List.empty,
    config: Config = Config()
) {

  def getState(playerId: Option[Long] = None): PublicGameState = {
    val gameState = PublicGameState(
      code = code,
      players = players,
      currentState = currentState
    )

    val playerGameState = playerId.map { id =>
      currentState match {
        case GameStates.AnsweringPrompts =>
          val round = rounds(currentRound)
          val userAnswers = round.userAnswers(id)
          gameState.copy(
            prompt = userAnswers.find(_.answer.isEmpty).map(_.prompt)
          )

        case _ =>
          gameState
      }
    }

    playerGameState.getOrElse(gameState)
  }

  def addPlayer(player: Player): Instance = {
    if (players.length >= config.maxPlayers) {
      throw new PlayerLimitReachedException()
    }

    if (currentState != GameStates.WaitingForPlayers) {
      throw new GameInProgressException()
    }

    val newPlayer = Player(
      id = Some(nextPlayerID),
      name = player.name,
      score = Some(0)
    )

    copy(players = players :+ newPlayer, nextPlayerID = nextPlayerID + 1)
  }

  def updatePlayer(player: Player): Instance = {
    val playerIndex = players.indexWhere(_.id == player.id)
    val newPlayers = players.updated(playerIndex, player)

    copy(players = newPlayers)
  }

  def removePlayer(playerId: Long): Instance = {
    val newPlayers = players.filterNot(_.id == Some(playerId))

    copy(players = newPlayers)
  }

  def newRound: Round = {
    Round(List.empty, Map.empty)
  }

  def advanceState: Instance = {
    currentState match {
      case GameStates.WaitingForPlayers =>
        copy(
          currentState = GameStates.AnsweringPrompts,
          currentRound = currentRound + 1,
          rounds = rounds :+ newRound
        )

      case GameStates.AnsweringPrompts =>
        copy(
          currentState = GameStates.VotingOnAnswers
        )

    }

  }
}
