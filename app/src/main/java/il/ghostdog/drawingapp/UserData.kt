package il.ghostdog.drawingapp

class UserData {
    lateinit var nickname: String
    lateinit var email: String

    constructor() {}

    constructor(nickname: String, email: String){
        this.nickname = nickname
        this.email = email
    }
}