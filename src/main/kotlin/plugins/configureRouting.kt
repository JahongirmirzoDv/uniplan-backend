package plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import repositories.TimeTableRepository
import routes.timeTableRoutes
import services.ExcelService
import services.FirestoreService

fun Application.configureRouting() {
    val firestoreService = FirestoreService()
    val excelService = ExcelService()

    routing {
        timeTableRoutes(excelService, firestoreService)

        // Static file serving for testing/debugging purposes
        static("/static") {
            resources("static")
        }
    }
}