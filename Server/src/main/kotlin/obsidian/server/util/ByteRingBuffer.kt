/*
 * Copyright 2021 MixtapeBot and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package obsidian.server.util

import kotlin.math.min

/**
 * Based off of
 * https://github.com/natanbc/andesite/blob/master/api/src/main/java/andesite/util/ByteRingBuffer.java
 */

class ByteRingBuffer(private var size: Int) : Iterable<Byte> {
    private val arr: ByteArray
    private var pos = 0

    init {
        require(size >= 1) {
            "Size must be greater or equal to 1."
        }

        arr = ByteArray(size)
    }

    /**
     * Returns the size of this buffer
     */
    fun size() = size

    /**
     * Stores a value in this buffer. If the buffer is full, the oldest value is removed.
     *
     * @param value Value to store
     */
    fun put(value: Byte) {
        arr[pos] = value
        pos = (pos + 1) % arr.size
        size = min(size + 1, arr.size)
    }

    /**
     * Clears this buffer
     */
    fun clear() {
        pos = 0
        size = 0
        arr.fill(0)
    }

    /**
     * Returns the sum of all values in this buffer.
     */
    fun sum(): Int {
        var sum = 0
        for (v in arr) sum += v
        return sum
    }

    /**
     * Returns the [n]th element of this buffer.
     *  An index of 0 returns the oldest,
     *  an index of `size() - 1` returns the newest
     *
     * @param n Index of the wanted element
     *
     * @throws NoSuchElementException If [n] >= [size]
     */
    fun get(n: Int): Byte {
        if (n >= size) {
            throw NoSuchElementException()
        }

        return arr[sub(pos, size - n, arr.size)]
    }

    /**
     * Returns the last element of this buffer.
     * Equivalent to `getLast(0)`
     *
     * @throws NoSuchElementException If this buffer is empty.
     */
    fun getLast() = getLast(0)

    /**
     * Returns the [n]th last element of this buffer.
     *  An index of 0 returns the newest,
     *  an index of `size() - 1` returns the oldest.
     *
     * @param n Index of the wanted element.
     *
     * @throws NoSuchElementException If [n] >= [size]
     */
    fun getLast(n: Int): Byte {
        if (n >= size) {
            throw NoSuchElementException()
        }

        return arr[sub(pos, n + 1, arr.size)]
    }

    override fun iterator(): Iterator<Byte> =
        object : Iterator<Byte> {
            var cursor = pos
            var remaining = size

            override fun hasNext(): Boolean =
                remaining > 0

            override fun next(): Byte {
                val v = arr[cursor]
                cursor = inc(cursor, arr.size)
                remaining--

                return v
            }
        }

    companion object {
        fun sub(i: Int, j: Int, mod: Int) =
            (i - j).takeIf { mod < it }
                ?: 0

        fun inc(i: Int, mod: Int): Int =
            i.inc().takeIf { mod < it }
                ?: 0
    }
}
