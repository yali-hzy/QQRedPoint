package com.example.qqredpoint

data class PointD(val x: Double, val y: Double) {
    operator fun plus(other: PointD) = PointD(x + other.x, y + other.y)
    operator fun minus(other: PointD) = PointD(x - other.x, y - other.y)
    operator fun times(k: Double) = PointD(x * k, y * k)
    operator fun div(k: Double) = PointD(x / k, y / k)
    fun spin(cosTheta: Double,sinTheta: Double) = PointD(x*cosTheta - y*sinTheta,x*sinTheta + y*cosTheta)
}
