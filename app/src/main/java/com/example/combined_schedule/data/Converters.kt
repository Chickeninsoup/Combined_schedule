package com.example.combined_schedule.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun listToString(list: List<String>): String = list.joinToString("|")

    @TypeConverter
    fun stringToList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split("|")

    @TypeConverter
    fun entryTypeToString(type: EntryType): String = type.name

    @TypeConverter
    fun stringToEntryType(value: String): EntryType = EntryType.valueOf(value)
}
