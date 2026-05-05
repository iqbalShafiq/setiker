package co.ratmo.setiker

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform