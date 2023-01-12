package il.ghostdog.drawingapp

data class GuessMessageRData(
    val name: String,
    val guess: String
) {
    constructor() : this("", ""){}
}
