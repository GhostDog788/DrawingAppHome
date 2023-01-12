package il.ghostdog.drawingapp

import android.text.BoringLayout

class PlayerData {

    lateinit var name: String
    var points: Int = 0
    var answeredCorrectly: Boolean = false

    constructor() {}

    constructor(name: String){
        this.name = name
    }
}