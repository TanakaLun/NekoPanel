package io.tl.nekopanel.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SettingsDao {

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<SettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: SettingsEntity)

    @Transaction
    suspend fun getString(key: String, default: String = ""): String {
        return get(key) ?: default
    }

    @Transaction
    suspend fun getInt(key: String, default: Int = 0): Int {
        return get(key)?.toIntOrNull() ?: default
    }

    @Transaction
    suspend fun getLong(key: String, default: Long = 0L): Long {
        return get(key)?.toLongOrNull() ?: default
    }

    @Transaction
    suspend fun getBoolean(key: String, default: Boolean = false): Boolean {
        return get(key)?.toBooleanStrictOrNull() ?: default
    }

    @Transaction
    suspend fun putString(key: String, value: String) {
        put(SettingsEntity(key, value))
    }
}
