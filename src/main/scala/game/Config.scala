package game

case class Config(
    codeLenght: Int = 4,
    minPlayers: Int = 3,
    maxPlayers: Int = 8,
    rounds: Int = 2,
    promptsPerRound: Int = 2
)
