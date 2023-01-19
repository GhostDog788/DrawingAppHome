package il.ghostdog.drawingapp

import java.io.Serializable

class PlayerData : Serializable{

    lateinit var name: String
    var points: Int = 0
    var answeredCorrectly: Boolean = false

    constructor() {}

    constructor(name: String){
        this.name = name
    }
}