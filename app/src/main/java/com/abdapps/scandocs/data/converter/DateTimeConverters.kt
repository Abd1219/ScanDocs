package com.abdapps.scandocs.data.converter

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date // Importación para java.util.Date

/**
 * Convertidores para tipos de fecha y hora en Room
 */
class DateTimeConverters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // Convertidores existentes para java.time.LocalDateTime
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.format(formatter)
    }

    // NUEVOS Convertidores para java.util.Date
    @TypeConverter
    fun fromDateTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToDateTimestamp(date: Date?): Long? {
        return date?.time
    }
}