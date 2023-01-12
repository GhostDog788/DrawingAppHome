package il.ghostdog.drawingapp

import androidx.annotation.DrawableRes

data class PlayerRViewData(
    val userId: String,
    val name: String,
    @DrawableRes val profilePic: Int,
    var isLeader: Boolean
) {
    constructor(userId: String, playerData: PlayerData, isLeader: Boolean) : this(
        userId,
        playerData.name, R.drawable.ic_brush, isLeader
    )
}
