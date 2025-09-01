package com.abdapps.scandocs.data.converter

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Conversores para Room Database
 * 
 * Esta clase proporciona métodos para convertir tipos de datos complejos
 * a formatos que Room puede almacenar en la base de datos SQLite.
 * 
 * Conversores implementados:
 * - LocalDateTime <-> String
 * - List<String> <-> String (para tags)
 */
class Converters {
    
    // Formato de fecha y hora estándar
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    /**
     * Convierte LocalDateTime a String para almacenamiento en Room
     * 
     * @param dateTime Fecha y hora a convertir
     * @return String representando la fecha y hora
     */
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(formatter)
    }
    
    /**
     * Convierte String a LocalDateTime desde Room
     * 
     * @param dateTimeString String representando la fecha y hora
     * @return LocalDateTime parseado
     */
    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let { LocalDateTime.parse(it, formatter) }
    }
    
    /**
     * Convierte List<String> a String para almacenamiento en Room
     * 
     * @param tags Lista de etiquetas
     * @return String con etiquetas separadas por comas
     */
    @TypeConverter
    fun fromTagsList(tags: List<String>?): String? {
        return tags?.joinToString(",")
    }
    
    /**
     * Convierte String a List<String> desde Room
     * 
     * @param tagsString String con etiquetas separadas por comas
     * @return Lista de etiquetas
     */
    @TypeConverter
    fun toTagsList(tagsString: String?): List<String>? {
        return tagsString?.split(",")?.filter { it.isNotBlank() }
    }
}
