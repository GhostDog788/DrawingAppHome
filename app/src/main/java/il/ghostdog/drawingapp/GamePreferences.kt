package il.ghostdog.drawingapp


class GamePreferences {
    var status: GameStatus = GameStatus.preparing
    var language: String = "english"
    var rounds: Int = 2
    var turnTime: Int = 15

    constructor(){}

    constructor(status: GameStatus){
        this.status = status
    }
}