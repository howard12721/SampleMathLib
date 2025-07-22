import kotlin.math.abs

/**
 * 有理数を表すクラス
 */
class Rational(numerator: Long, denominator: Long) : Number(), Comparable<Rational> {
    val n: Long
    val d: Long

    init {
        require(denominator != 0L) { "Denominator cannot be zero." }
        val g = gcd(numerator, denominator) * if (denominator < 0) -1 else 1
        this.n = numerator / g
        this.d = denominator / g
    }

    operator fun plus(other: Rational): Rational =
        Rational(this.n * other.d + other.n * this.d, this.d * other.d)

    operator fun minus(other: Rational): Rational =
        Rational(this.n * other.d - other.n * this.d, this.d * other.d)

    operator fun times(other: Rational): Rational =
        Rational(this.n * other.n, this.d * other.d)

    operator fun div(other: Rational): Rational =
        Rational(this.n * other.d, this.d * other.n)

    operator fun unaryMinus(): Rational = Rational(-n, d)

    override fun toDouble(): Double = n.toDouble() / d.toDouble()

    override fun toFloat(): Float = n.toFloat() / d.toFloat()

    override fun toLong(): Long = n / d

    override fun toInt(): Int = (n / d).toInt()

    override fun toShort(): Short = (n / d).toShort()

    override fun toByte(): Byte = (n / d).toByte()

    override fun compareTo(other: Rational): Int =
        (this.n.toBigInteger() * other.d.toBigInteger())
            .compareTo((other.n.toBigInteger() * this.d.toBigInteger()))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Rational) return false
        return this.n == other.n && this.d == other.d
    }

    private tailrec fun gcd(a: Long, b: Long): Long =
        if (b == 0L) abs(a) else gcd(b, a % b)

    override fun hashCode(): Int {
        var result = n.hashCode()
        result = 31 * result + d.hashCode()
        return result
    }

    override fun toString(): String = if (d == 1L) n.toString() else "$n/$d"
}
