package il.ghostdog.drawingapp

import android.graphics.Bitmap

data class PlayerRViewData(
    val userId: String,
    val name: String,
    var profilePic: Bitmap?,
    var isLeader: Boolean
) {
    constructor(userId: String, playerData: PlayerData, profilePic: Bitmap?, isLeader: Boolean) : this(
        userId,
        playerData.name, profilePic, isLeader
    )
}
