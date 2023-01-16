package il.ghostdog.drawingapp


class GamePreferences {
    var status: GameStatus = GameStatus.preparing
    var language: String = "english"
    var rounds: Int = 3
    var turnTime: Int = 60

    constructor(){}

    constructor(status: GameStatus){
        this.status = status
    }
}