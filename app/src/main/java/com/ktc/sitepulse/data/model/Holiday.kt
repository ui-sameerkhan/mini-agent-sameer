package com.ktc.sitepulse.data.model

/** Firestore: holidays/{date} — doc id is the "YYYY-MM-DD" date itself, so a given calendar
 * date can only ever have one holiday entry. A worker with no attendance on a holiday date
 * shows as "HOLIDAY" instead of "ABSENT" on the Absent Report — see ReportEngine. */
data class Holiday(
    val date: String = "",
    val name: String = "",
    val addedBy: String = "",
    val addedAt: String = "",
)
