package com.rafl.mapcr

import java.util.*
import kotlin.collections.ArrayList

fun<T> MutableList<T>.moveUp(i: Int): Int {
    if (size == 1) return i
    return if (i == 0) {
        val rest = ArrayList(subList(1, size))
        val first = this[i]
        this.indices.forEach {
            this[it] = if (it == lastIndex) first else rest[it]
        }
        lastIndex
    } else {
        Collections.swap(this, i, i-1)
        i-1
    }
}

fun<T> MutableList<T>.moveDown(i: Int): Int {
    if (size == 1) return i
    return if (i == lastIndex) {
        val list = ArrayList<T>()
        list.add(get(i))
        list.addAll(subList(0, lastIndex))
        this.indices.forEach {
            this[it] = list[it]
        }
        0
    } else {
        Collections.swap(this, i, i+1)
        i+1
    }
}