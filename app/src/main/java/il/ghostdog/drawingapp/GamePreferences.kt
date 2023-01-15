package il.ghostdog.drawingapp


class GamePreferences {
    var status: GameStatus = GameStatus.preparing
    var language: String? = "english"

    constructor(){}

    constructor(status: GameStatus){
        this.status = status
    }
}