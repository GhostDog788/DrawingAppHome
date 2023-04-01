package il.ghostdog.drawingapp

class UserData {
    lateinit var nickname: String
    lateinit var email: String
    var money: Int = 0
    var friendsList: ArrayList<String> = ArrayList()
    var lastSeen: String? = null
    var activeGame: String? = null
    var token: String = ""

    constructor() {}

    constructor(nickname: String, email: String){
        this.nickname = nickname
        this.email = email
    }
}