package il.ghostdog.drawingapp

import android.graphics.Bitmap

class UserData {
    lateinit var nickname: String
    lateinit var email: String
    lateinit var profilePic: Bitmap

    constructor() {}

    constructor(nickname: String, email: String, profilePic: Bitmap){
        this.nickname = nickname
        this.email = email
        this.profilePic = profilePic
    }
}