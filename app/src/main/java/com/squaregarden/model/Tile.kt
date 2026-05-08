package com.squaregarden.model

enum class TileColor {
    RED, BLUE, YELLOW, GREEN, ORANGE
}

data class Tile(
    val color: TileColor,
    val frozen: Boolean = false,
    val redo: Boolean = false,
    val shuffleToken: Boolean = false,
    val passthroughToken: Boolean = false,
    val unfreezeToken: Boolean = false
)

data class CellPos(val row: Int, val col: Int)
