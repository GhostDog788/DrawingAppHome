package il.ghostdog.drawingapp

class UserData {
    lateinit var nickname: String
    lateinit var email: String
    var money: Int = 0

    constructor() {}

    constructor(nickname: String, email: String){
        this.nickname = nickname
        this.email = email
    }
}