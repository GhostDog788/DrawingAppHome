package il.ghostdog.drawingapp

import androidx.annotation.DrawableRes

data class PlayerRGameViewData(
    val userId: String,
    val name: String,
    var points: Int,
    var isDrawer: Boolean,
    var answeredCorrectly: Boolean
) {
    constructor(userId: String, playerData: PlayerData, isDrawer: Boolean) : this(
        userId,
        playerData.name,
        playerData.points,
        isDrawer,
        playerData.answeredCorrectly
    )
}
