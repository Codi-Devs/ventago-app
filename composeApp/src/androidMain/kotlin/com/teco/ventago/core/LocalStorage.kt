package com.teco.ventago.core

import android.content.Context
import android.content.SharedPreferences

actual open class LocalStorage(
    context: Context,
    fileName: String = "tecomenu-shared",
) {
    private val sp: SharedPreferences
    init {
        sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    }

    /**
     * Saves a string value in the SharedPreferences.
     * @param key The key to store
     * @param stringValue The value to store
     * @return True or false, depending on whether the value has been stored in the SharedPreferences
     */
    actual fun set(key: String, stringValue: String): Boolean {
        return sp
            .edit()
            .putString(key, stringValue)
            .commit()
    }

    /**
     * Saves an int value in the SharedPreferences.
     * @param key The key to store
     * @param intValue The value to store
     * @return True or false, depending on whether the value has been stored in the SharedPreferences
     */
    actual fun set(key: String, intValue: Int): Boolean {
        return sp
            .edit()
            .putInt(key, intValue)
            .commit()
    }

    /**
     * Saves a long value in the SharedPreferences.
     * @param key The key to store
     * @param longValue The value to store
     * @return True or false, depending on whether the value has been stored in the SharedPreferences
     */
    actual fun set(key: String, longValue: Long): Boolean {
        return sp
            .edit()
            .putLong(key, longValue)
            .commit()
    }

    /**
     * Saves a float value in the SharedPreferences.
     * @param key The key to store
     * @param floatValue The value to store
     * @return True or false, depending on whether the value has been stored in the SharedPreferences
     */
    actual fun set(key: String, floatValue: Float): Boolean {
        return sp
            .edit()
            .putFloat(key, floatValue)
            .commit()
    }

    /**
     * Saves a double value in the SharedPreferences.
     * @param key The key to store
     * @param doubleValue The value to store
     * @return True or false, depending on whether the value has been stored in the SharedPreferences
     */
    actual fun set(key: String, doubleValue: Double): Boolean {
        return sp
            .edit()
            .putLong(key, doubleValue.toRawBits())
            .commit()
    }

    /**
     * Saves a boolean value in the SharedPreferences.
     * @param key The key to store
     * @param boolValue The value to store
     * @return True or false, depending on whether the value has been stored in the SharedPreferences
     */
    actual fun set(key: String, boolValue: Boolean): Boolean {
        return sp
            .edit()
            .putBoolean(key, boolValue)
            .commit()
    }

    /**
     * Checks if object with key exists in the SharedPreferences.
     * @param forKey The key to query
     * @return True or false, depending on whether the value has been stored in the SharedPreferences
     */
    actual fun existsObject(forKey: String): Boolean {
        return sp.contains(forKey)
    }

    /**
     * Returns the string value of an object in the SharedPreferences.
     * @param forKey The key to query
     * @return The stored string value, or null if it is missing
     */
    actual fun string(forKey: String): String? {
        return sp.getString(forKey, null)
    }

    /**
     * Returns the int value of an object in the SharedPreferences.
     * @param forKey The key to query
     * @return The stored int value, or null if it is missing
     */
    actual fun int(forKey: String): Int? {
        return if (existsObject(forKey)) {
            sp.getInt(forKey, Int.MIN_VALUE)
        } else {
            null
        }
    }

    /**
     * Returns the long value of an object in the SharedPreferences.
     * @param forKey The key to query
     * @return The stored long value, or null if it is missing
     */
    actual fun long(forKey: String): Long? {
        return if (existsObject(forKey)) {
            sp.getLong(forKey, Long.MIN_VALUE)
        } else {
            null
        }
    }

    /**
     * Returns the float value of an object in the SharedPreferences.
     * @param forKey The key to query
     * @return The stored float value, or null if it is missing
     */
    actual fun float(forKey: String): Float? {
        return if (existsObject(forKey)) {
            sp.getFloat(forKey, Float.MIN_VALUE)
        } else {
            null
        }
    }

    /**
     * Returns the double value of an object in the SharedPreferences.
     * @param forKey The key to query
     * @return The stored double value, or null if it is missing
     */
    actual fun double(forKey: String): Double? {
        return if (existsObject(forKey)) {
            Double.fromBits(sp.getLong(forKey, Double.MIN_VALUE.toRawBits()))
        } else {
            null
        }
    }

    /**
     * Returns the boolean value of an object in the SharedPreferences.
     * @param forKey The key to query
     * @return The stored boolean value, or null if it is missing
     */
    actual fun bool(forKey: String): Boolean? {
        return if (existsObject(forKey)) {
            sp.getBoolean(forKey, false)
        } else {
            null
        }
    }

    /**
     * Returns all keys of the objects in the SharedPreferences.
     * @return A list with all keys
     */
    actual fun allKeys(): List<String> {
        return sp.all.map { it.key }
    }

    /**
     * Deletes object with the given key from the SharedPreferences.
     * @param forKey The key to query
     * @return True or false, depending on whether the object has been deleted
     */
    actual fun deleteObject(forKey: String): Boolean {
        return sp
            .edit()
            .remove(forKey)
            .commit()
    }

    /**
     * Deletes all objects from the SharedPreferences.
     * @return True or false, depending on whether the objects have been deleted
     */
    actual fun clear(): Boolean {
        return sp
            .edit()
            .clear()
            .commit()
    }

}