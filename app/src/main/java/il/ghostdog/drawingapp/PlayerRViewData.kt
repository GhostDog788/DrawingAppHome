package il.ghostdog.drawingapp

import androidx.annotation.DrawableRes

data class PlayerRViewData(
    val name: String,
    @DrawableRes val profilePic: Int,
    val isLeader: Boolean
) {
    constructor(playerData: PlayerData, isLeader: Boolean) : this(
        playerData.name, R.drawable.ic_brush, isLeader
    )
}
