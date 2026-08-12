package com.ktc.sitepulse.domain

import com.ktc.sitepulse.data.model.Worker

/** Mirrors findWorker()/verifyId()'s fuzzy-match behavior from the web app. */
object WorkerSearch {
    fun findExact(workers: List<Worker>, id: String): Worker? =
        workers.find { it.id == id.trim() }

    fun suggest(workers: List<Worker>, query: String, limit: Int = 8): List<Worker> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return workers.filter { it.name.lowercase().contains(q) || it.id.contains(query.trim()) }.take(limit)
    }
}
