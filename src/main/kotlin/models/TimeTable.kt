package models

import kotlinx.serialization.Serializable

@Serializable
data class TimeTable(
    val id: String = "",
    val group: String = "",
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val date:String = "",
    val className: String = "",
    val teacherName: String = "",
    val room: String = ""
)

@Serializable
data class UploadResponse(
    val message: String = "",
    val note: String = "",
    val error: String = "",
    val count: Int = 0,
    val ids: List<String> = emptyList<String>(),
)


//object TimeTables : Table() {
//    val id = integer("id").autoIncrement()
//    val group = varchar("group_name", 50)
//    val day = varchar("day", 20)
//    val time = varchar("time", 20)
//    val className = varchar("class_name", 100)
//    val teacherName = varchar("teacher_name", 100)
//    val room = varchar("room", 50)
//
//    override val primaryKey = PrimaryKey(id)
//}