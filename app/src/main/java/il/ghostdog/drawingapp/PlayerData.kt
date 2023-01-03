package il.ghostdog.drawingapp

class PlayerData {

    lateinit var name: String
    var points: Int = 0

    constructor() {}

    constructor(name: String){
        this.name = name
    }
}