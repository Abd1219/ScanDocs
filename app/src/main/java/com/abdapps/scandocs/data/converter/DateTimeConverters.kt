package com.abdapps.scandocs.data.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Convertidores para tipos de fecha y hora en Room
 * Compatible con API 24+
 */
class DateTimeConverters {

    /**
     * Convierte Long timestamp a Date
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Convierte Date a Long timestamp
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}