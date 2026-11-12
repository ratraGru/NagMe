package com.gru.nagme.domain

/** Sumber waktu yang bisa diganti (untuk Mode Demo dan unit test) — Bagian 7.1 A3. */
fun interface Clock {
    fun now(): Long
}

object RealClock : Clock {
    override fun now(): Long = System.currentTimeMillis()
}
