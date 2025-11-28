/*
 * Copyright 2023-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.remote.classes

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

interface InternalConcurrentHashMap<K : Any, V : Any> {
    fun put(key: K, value: V): V?

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    fun merge(key: K, value: V, remappingFunction: (V, V) -> V): V

    fun computeIfAbsent(key: K, computeValue: () -> V): V

    operator fun get(key: K): V?

    fun remove(key: K): V?

    fun clear()

    fun containsKey(key: K): Boolean

    val entries: Set<Entry<K, V>>

    val keys: Collection<K>

    val values: Collection<V>

    data class Entry<K : Any, V : Any>(
        val key: K,
        val value: V,
    )
}

fun <K : Any, V : Any> InternalConcurrentHashMap(): InternalConcurrentHashMap<K, V> = SynchronizedHashMap()

internal class SynchronizedHashMap<K : Any, V: Any> : InternalConcurrentHashMap<K, V>, SynchronizedObject() {
    private val map = hashMapOf<K, V>()

    override fun put(key: K, value: V): V? = synchronized(this) {
        map.put(key, value)
    }

    override fun merge(key: K, value: V, remappingFunction: (V, V) -> V): V = synchronized(this) {
        val old = map[key]
        if (old == null) {
            map[key] = value
            value
        } else {
            val new = remappingFunction(old, value)
            map[key] = new
            new
        }
    }

    override fun computeIfAbsent(key: K, computeValue: () -> V): V = synchronized(this) {
        map[key] ?: computeValue().also { map[key] = it }
    }

    override operator fun get(key: K): V? = synchronized(this) {
        map[key]
    }

    override fun remove(key: K): V? = synchronized(this) {
        map.remove(key)
    }

    override fun clear() = synchronized(this) {
        map.clear()
    }

    override fun containsKey(key: K): Boolean = synchronized(this) {
        map.containsKey(key)
    }

    override val entries: Set<InternalConcurrentHashMap.Entry<K, V>>
        get() = synchronized(this) { map.entries }.map { InternalConcurrentHashMap.Entry(it.key, it.value) }.toSet()

    override val keys: Collection<K>
        get() = synchronized(this) { map.keys }

    override val values: Collection<V>
        get() = synchronized(this) { map.values }
}
