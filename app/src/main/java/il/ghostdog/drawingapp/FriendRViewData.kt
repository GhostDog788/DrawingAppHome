package il.ghostdog.drawingapp

import android.graphics.Bitmap

data class FriendRViewData(
    val userId: String,
    val name: String,
    var profilePic: Bitmap?,
) {
    constructor(userId: String, userData: UserData, profilePic: Bitmap?) : this(
        userId,
        userData.nickname,
        profilePic
    )
}
