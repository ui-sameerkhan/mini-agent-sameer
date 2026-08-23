package com.ktc.sitepulse.domain

/**
 * UAE MOHRE (Ministry of Human Resources and Emiratisation) private-sector public holiday
 * calendar, 2026-2027. Fixed-Gregorian-date holidays (New Year, Commemoration Day, National
 * Day) are certain. Islamic (Hijri) calendar holidays — Eid Al Fitr, Eid Al Adha, Hijri New
 * Year, Mawlid Al Nabi — depend on official moon sighting; MOHRE confirms the exact date only
 * days beforehand. The 2026 Hijri New Year and Mawlid dates below were confirmed by MOHRE
 * itself; the rest are the best available estimate as of when this list was compiled and are
 * marked "(unconfirmed)" — verify against the current MOHRE circular before relying on them
 * for payroll, and correct via Delete + Add Holiday if the actual date differs by a day.
 */
object UaeHolidaysMohre {
    data class Entry(val date: String, val name: String)

    val ENTRIES_2026_2027: List<Entry> = listOf(
        // ---- 2026 ----
        Entry("2026-01-01", "New Year's Day"),
        Entry("2026-03-19", "Eid Al Fitr (Day 1, unconfirmed)"),
        Entry("2026-03-20", "Eid Al Fitr (Day 2, unconfirmed)"),
        Entry("2026-03-21", "Eid Al Fitr (Day 3, unconfirmed)"),
        Entry("2026-03-22", "Eid Al Fitr (Day 4, unconfirmed)"),
        Entry("2026-05-26", "Arafat (Waqfat Arafah) Day (unconfirmed)"),
        Entry("2026-05-27", "Eid Al Adha (Day 1, unconfirmed)"),
        Entry("2026-05-28", "Eid Al Adha (Day 2, unconfirmed)"),
        Entry("2026-05-29", "Eid Al Adha (Day 3, unconfirmed)"),
        Entry("2026-06-15", "Hijri New Year"),
        Entry("2026-08-28", "Mawlid Al Nabi (Prophet Muhammad's Birthday)"),
        Entry("2026-12-01", "Commemoration Day"),
        Entry("2026-12-02", "UAE National Day (Day 1)"),
        Entry("2026-12-03", "UAE National Day (Day 2)"),
        // ---- 2027 ----
        Entry("2027-01-01", "New Year's Day"),
        Entry("2027-03-09", "Eid Al Fitr (Day 1, unconfirmed)"),
        Entry("2027-03-10", "Eid Al Fitr (Day 2, unconfirmed)"),
        Entry("2027-03-11", "Eid Al Fitr (Day 3, unconfirmed)"),
        Entry("2027-03-12", "Eid Al Fitr (Day 4, unconfirmed)"),
        Entry("2027-05-14", "Arafat (Waqfat Arafah) Day (unconfirmed)"),
        Entry("2027-05-15", "Eid Al Adha (Day 1, unconfirmed)"),
        Entry("2027-05-16", "Eid Al Adha (Day 2, unconfirmed)"),
        Entry("2027-05-17", "Eid Al Adha (Day 3, unconfirmed)"),
        Entry("2027-06-06", "Hijri New Year (unconfirmed)"),
        Entry("2027-08-14", "Mawlid Al Nabi (unconfirmed)"),
        Entry("2027-12-01", "Commemoration Day"),
        Entry("2027-12-02", "UAE National Day (Day 1)"),
        Entry("2027-12-03", "UAE National Day (Day 2)"),
    )
}
