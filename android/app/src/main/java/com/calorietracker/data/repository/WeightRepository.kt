package com.calorietracker.data.repository

import com.calorietracker.data.db.dao.WeightLogDao
import com.calorietracker.data.db.entity.WeightLogEntity
import com.calorietracker.data.model.WeightLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepository @Inject constructor(private val dao: WeightLogDao) {

    fun getAllWeightLogs(): Flow<List<WeightLog>> =
        dao.getAllWeightLogs().map { it.map(WeightLogEntity::toDomain) }

    suspend fun getForDate(dateMillis: Long): WeightLog? =
        dao.getForDate(dateMillis)?.toDomain()

    suspend fun insert(log: WeightLog) =
        dao.insert(log.toEntity())

    suspend fun deleteById(id: String) =
        dao.deleteById(id)
}

private fun WeightLogEntity.toDomain() = WeightLog(id, date, weight)
private fun WeightLog.toEntity() = WeightLogEntity(id, date, weight)
