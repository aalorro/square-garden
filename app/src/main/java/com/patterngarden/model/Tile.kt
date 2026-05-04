package com.patterngarden.model

enum class TileColor {
    RED, BLUE, YELLOW, GREEN, ORANGE
}

data class Tile(val color: TileColor, val frozen: Boolean = false)

data class CellPos(val row: Int, val col: Int)
