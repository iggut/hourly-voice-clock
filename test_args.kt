import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AnnouncementScheduler {
    companion object {
        fun getNextTopOfHour(from: LocalDateTime = LocalDateTime.now()): LocalDateTime {
            return from.plusHours(1).truncatedTo(ChronoUnit.HOURS)
        }
    }
}

fun main() {
    val now = LocalDateTime.now()
    val next = AnnouncementScheduler.getNextTopOfHour(now)
    println(next)
}
