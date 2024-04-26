package game

class PlayerNotFoundException(
    message: String = "player not found",
    cause: Throwable = null
) extends Exception(message, cause)

class PlayerLimitReachedException(
    message: String = "player limit reached",
    cause: Throwable = null
) extends Exception(message, cause)

class PlayerMinimumNotMetException(
    message: String = "players below minimum",
    cause: Throwable = null
) extends Exception(message, cause)

class GameInProgressException(
    message: String = "game in progress",
    cause: Throwable = null
) extends Exception(message, cause)
