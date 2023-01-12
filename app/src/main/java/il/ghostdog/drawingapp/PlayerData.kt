package il.ghostdog.drawingapp

class PlayerData {

    lateinit var name: String
    var points: Int = 1234567890

    constructor() {}

    constructor(name: String){
        this.name = name
    }
}