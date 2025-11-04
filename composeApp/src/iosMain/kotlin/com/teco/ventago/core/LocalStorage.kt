package com.teco.ventago.core

import platform.Foundation.NSNumber
import platform.Foundation.NSUserDefaults
import platform.Foundation.setValue
import platform.Foundation.valueForKey

actual open class LocalStorage() {

    /**
     * Saves a string value in the store.
     * @param key The key to store
     * @param stringValue The value to store
     */
    actual fun set(key: String, stringValue: String): Boolean {
        NSUserDefaults.standardUserDefaults.setValue(stringValue, forKey = key)
        return true
    }

    /**
     * Saves an int value in the store.
     * @param key The key to store
     * @param intValue The value to store
     */
    actual fun set(key: String, intValue: Int): Boolean {
        NSUserDefaults.standardUserDefaults.setValue(NSNumber(int = intValue), forKey = key)
        return true
    }

    /**
     * Saves a long value in the store.
     * @param key The key to store
     * @param longValue The value to store
     */
    actual fun set(key: String, longValue: Long): Boolean {
        NSUserDefaults.standardUserDefaults.setValue(NSNumber(long = longValue), forKey = key)
        return true
    }

    /**
     * Saves a float value in the store.
     * @param key The key to store
     * @param floatValue The value to store
     */
    actual fun set(key: String, floatValue: Float): Boolean {
        NSUserDefaults.standardUserDefaults.setValue(NSNumber(float = floatValue), forKey = key)
        return true
    }

    /**
     * Saves a double value in the store.
     * @param key The key to store
     * @param doubleValue The value to store
     */
    actual fun set(key: String, doubleValue: Double): Boolean {
        NSUserDefaults.standardUserDefaults.setValue(NSNumber(double = doubleValue), forKey = key)
        return true
    }

    /**
     * Saves a boolean value in the store.
     * @param key The key to store
     * @param boolValue The value to store
     */
    actual fun set(key: String, boolValue: Boolean): Boolean {
        NSUserDefaults.standardUserDefaults.setBool(boolValue, forKey = key)
        return true
    }

    /**
     * Checks if object with key exists in the store.
     * @param forKey The key to query
     * @return True or false, depending on wether it is in the store or not
     */
    actual fun existsObject(forKey: String): Boolean {
        return NSUserDefaults.standardUserDefaults.valueForKey(forKey) != null
    }

    /**
     * Returns the string value of an object in the store.
     * @param forKey The key to query
     * @return The stored string value
     */
    actual fun string(forKey: String): String? {
        return NSUserDefaults.standardUserDefaults.valueForKey(forKey) as String?
    }

    /**
     * Returns the int value of an object in the store.
     * @param forKey The key to query
     * @return The stored int value
     */
    actual fun int(forKey: String): Int? {
        val number = NSUserDefaults.standardUserDefaults.valueForKey(forKey) as NSNumber?
        return number?.intValue
    }

    /**
     * Returns the long value of an object in the store.
     * @param forKey The key to query
     * @return The stored long value
     */
    actual fun long(forKey: String): Long? {
        val number = NSUserDefaults.standardUserDefaults.valueForKey(forKey) as NSNumber?
        return number?.longValue
    }

    /**
     * Returns the double value of an object in the store.
     * @param forKey The key to query
     * @return The stored double lue
     */
    actual fun double(forKey: String): Double? {
        val number = NSUserDefaults.standardUserDefaults.valueForKey(forKey) as NSNumber?
        return number?.doubleValue
    }

    /**
     * Returns the float value of an object in the store.
     * @param forKey The key to query
     * @return The stored float value
     */
    actual fun float(forKey: String): Float? {
        val number = NSUserDefaults.standardUserDefaults.valueForKey(forKey) as NSNumber?
        return number?.floatValue
    }

    /**
     * Returns the boolean value of an object in the store.
     * @param forKey The key to query
     * @return The stored boolean value
     */
    actual fun bool(forKey: String): Boolean? {
        return NSUserDefaults.standardUserDefaults.boolForKey(forKey) as Boolean?
    }

    /**
     * Returns all keys of the stored objects.
     * @return A list with all keys
     */
    actual fun allKeys(): List<String> {
        return NSUserDefaults.standardUserDefaults.dictionaryRepresentation().keys.toList() as List<String>
    }

    /**
     * Deletes object with the given key from the store.
     * @param forKey The key to query
     * @return True or false, depending on whether the object has been deleted
     */
    actual fun deleteObject(forKey: String): Boolean {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(forKey)
        return true
    }

    /**
     * Deletes all objects from the store.
     * @return True or false, depending on whether the objects have been deleted
     */
    actual fun clear(): Boolean {
        NSUserDefaults.standardUserDefaults.dictionaryRepresentation().keys.forEach {
            if (it is String) {
                NSUserDefaults.standardUserDefaults.removeObjectForKey(it)
            }
        }
        return true
    }


}