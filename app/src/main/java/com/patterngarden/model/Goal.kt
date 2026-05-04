package com.patterngarden.model

sealed class Goal {
    abstract val color: TileColor
    abstract val description: String
    abstract val id: String

    data class Line(
        override val color: TileColor,
        val length: Int
    ) : Goal() {
        override val description get() = "Line of $length ${color.name.lowercase()}"
        override val id get() = "line_${color.name}_$length"
    }

    data class Square(
        override val color: TileColor
    ) : Goal() {
        override val description get() = "2x2 ${color.name.lowercase()} square"
        override val id get() = "square_${color.name}"
    }

    data class Shape(
        override val color: TileColor,
        val shapeType: ShapeType
    ) : Goal() {
        override val description get() = "${shapeType.label} of ${color.name.lowercase()}"
        override val id get() = "shape_${color.name}_${shapeType.name}"
    }
}

enum class ShapeType(val label: String, val offsets: List<CellPos>) {
    L_SHAPE("L-shape", listOf(
        CellPos(0, 0), CellPos(1, 0), CellPos(2, 0), CellPos(3, 0), CellPos(3, 1)
    )),
    T_SHAPE("T-shape", listOf(
        CellPos(0, 0), CellPos(0, 1), CellPos(0, 2), CellPos(1, 1), CellPos(2, 1)
    )),
    CROSS("Cross", listOf(
        CellPos(0, 1), CellPos(1, 0), CellPos(1, 1), CellPos(1, 2), CellPos(2, 1)
    )),
    Z_SHAPE("Z-shape", listOf(
        CellPos(0, 0), CellPos(0, 1), CellPos(1, 1), CellPos(1, 2)
    )),
    U_SHAPE("U-shape", listOf(
        CellPos(0, 0), CellPos(1, 0), CellPos(1, 1), CellPos(1, 2), CellPos(0, 2)
    ))
}
