package il.ghostdog.drawingapp


class GamePreferences {
    var status: GameStatus = GameStatus.preparing
    var public: Boolean = false
    var language: String = "english"
    var rounds: Int = 1
    var turnTime: Int = 60

    constructor(){}

    constructor(status: GameStatus){
        this.status = status
    }
}