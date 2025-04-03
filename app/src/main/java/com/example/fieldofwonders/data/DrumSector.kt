package com.example.fieldofwonders.data

sealed class DrumSector {
    data class Points(val value: Int) : DrumSector()
    object Double : DrumSector()
    object Zero : DrumSector()
    object Plus : DrumSector()
    object Bankrupt : DrumSector()

    override fun toString(): String = when (this) {
        is Points -> "$value"
        Double -> "Удвоение"
        Zero -> "Ноль"
        Plus -> "Плюс"
        Bankrupt -> "Банкрот"
    }
}