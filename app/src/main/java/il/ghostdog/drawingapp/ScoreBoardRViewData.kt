package il.ghostdog.drawingapp

import androidx.annotation.DrawableRes

data class ScoreBoardRViewData(
    val name: String,
    var points: Int,
) {
    constructor(playerData: PlayerData) : this(
        playerData.name,
        playerData.points,
    )
}
