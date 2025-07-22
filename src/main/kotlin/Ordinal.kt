import kotlin.math.max

operator fun Int.plus(other: Ordinal): Ordinal = Ordinal.fromInt(this) + other
operator fun Int.minus(other: Ordinal): Ordinal = Ordinal.fromInt(this) - other
operator fun Int.times(other: Ordinal): Ordinal = Ordinal.fromInt(this) * other
operator fun Int.div(other: Ordinal): Ordinal = Ordinal.fromInt(this) / other
operator fun Int.rem(other: Ordinal): Ordinal = Ordinal.fromInt(this) % other
fun Int.pow(other: Ordinal): Ordinal = Ordinal.fromInt(this).pow(other)

fun Int.pow(other: Int): Int {
    if (other < 0) throw IllegalArgumentException("Exponent must be non-negative.")
    var result = 1L
    val baseL = this.toLong()
    for (i in 1..other) {
        result *= baseL
        if (result > Int.MAX_VALUE) {
            throw ArithmeticException("Integer power $this^$other overflows Int")
        }
    }
    return result.toInt()
}

/**
 * 順序数を表すクラス
 */
class Ordinal private constructor(val terms: List<Term>) : Comparable<Ordinal> {
    /**
     * カントール標準形の項を表すデータクラス
     */
    data class Term(val exponent: Ordinal, val coefficient: Int) {
        init {
            require(coefficient >= 0) { "Coefficient must be non-negative." }
        }

        override fun toString(): String {
            if (exponent.isZero()) {
                return coefficient.toString()
            }

            val expStr = when {
                exponent == ONE -> "ω"
                exponent.terms.size > 1 -> "ω^(${exponent})"
                else -> "ω^${exponent}"
            }

            return if (coefficient == 1) expStr else "$expStr・$coefficient"
        }
    }

    init {
        if (terms.size > 1) {
            for (i in 0 until terms.size - 1) {
                require(terms[i].exponent > terms[i + 1].exponent) {
                    "Exponents must be in strictly descending order. Got ${terms[i].exponent} and ${terms[i + 1].exponent}"
                }
            }
        }
    }

    companion object {
        val ZERO = Ordinal(emptyList())

        val ONE = Ordinal(listOf(Term(ZERO, 1)))

        val OMEGA = Ordinal(listOf(Term(ONE, 1)))

        fun fromInt(n: Int): Ordinal {
            require(n >= 0) { "Cannot create an ordinal from a negative number." }
            return if (n == 0) ZERO else Ordinal(listOf(Term(ZERO, n)))
        }

        fun fromTerms(vararg terms: Term): Ordinal {
            return fromTerms(terms.toList())
        }

        fun fromTerms(terms: List<Term>): Ordinal {
            if (terms.isEmpty()) return ZERO

            val mergedTerms = terms.groupBy { it.exponent }.map { (exponent, termList) ->
                Term(exponent, termList.sumOf { it.coefficient })
            }.filter { it.coefficient > 0 }.sortedByDescending { it.exponent }

            return Ordinal(mergedTerms)
        }
    }

    fun isZero(): Boolean = terms.isEmpty()

    override fun compareTo(other: Ordinal): Int {
        val maxTerms = max(this.terms.size, other.terms.size)
        for (i in 0 until maxTerms) {
            val thisTerm = this.terms.getOrNull(i)
            val otherTerm = other.terms.getOrNull(i)

            if (thisTerm == null) return -1
            if (otherTerm == null) return 1

            val expCompare = thisTerm.exponent.compareTo(otherTerm.exponent)
            if (expCompare != 0) return expCompare

            val coeffCompare = thisTerm.coefficient.compareTo(otherTerm.coefficient)
            if (coeffCompare != 0) return coeffCompare
        }
        return 0
    }

    override fun toString(): String {
        return if (isZero()) "0" else terms.joinToString(" + ")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ordinal) return false
        return this.terms == other.terms
    }

    override fun hashCode(): Int {
        return terms.hashCode()
    }

    operator fun plus(other: Ordinal): Ordinal {
        if (this.isZero()) return other
        if (other.isZero()) return this

        return fromTerms(this.terms.takeWhile { it.exponent >= other.terms.first().exponent } + other.terms)
    }

    operator fun plus(other: Int): Ordinal {
        require(other >= 0) { "Cannot add a negative integer to an ordinal." }
        return this + fromInt(other)
    }

    operator fun minus(other: Ordinal): Ordinal {
        require(this >= other) { "Minuend must be greater than or equal to the subtrahend." }
        if (this == other) return ZERO
        if (other.isZero()) return this

        val commonLength = this.terms.asSequence().zip(other.terms.asSequence()).takeWhile { (a, b) -> a == b }.count()

        if (commonLength >= other.terms.size || this.terms[commonLength].exponent > other.terms[commonLength].exponent) {
            return fromTerms(*this.terms.drop(commonLength).toTypedArray())
        }

        val lhsTerm = this.terms[commonLength]
        val otherTerm = other.terms[commonLength]
        val newTerm = Term(lhsTerm.exponent, lhsTerm.coefficient - otherTerm.coefficient)

        return fromTerms(newTerm, *this.terms.drop(commonLength + 1).toTypedArray())
    }

    operator fun minus(other: Int): Ordinal {
        require(other >= 0) { "Cannot subtract a negative integer from an ordinal." }
        return this - fromInt(other)
    }

    operator fun times(other: Ordinal): Ordinal {
        if (this.isZero() || other.isZero()) return ZERO

        val lhsFirstTerm = this.terms.first()
        val rhsLastTerm = other.terms.last()

        return if (rhsLastTerm.exponent == ZERO) {
            fromTerms(
                *other.terms.filterNot { it.exponent == ZERO }
                    .map { Term(lhsFirstTerm.exponent + it.exponent, it.coefficient) }.toTypedArray(),
                Term(lhsFirstTerm.exponent, lhsFirstTerm.coefficient * rhsLastTerm.coefficient),
                *this.terms.drop(1).toTypedArray(),
            )
        } else {
            fromTerms(other.terms.map { Term(lhsFirstTerm.exponent + it.exponent, it.coefficient) })
        }
    }

    operator fun times(other: Int): Ordinal {
        require(other >= 0) { "Cannot multiply an ordinal by a negative integer." }
        return this * fromInt(other)
    }

    operator fun div(other: Ordinal): Ordinal {
        require(!other.isZero()) { "Cannot divide by zero ordinal." }
        if (other > this) return ZERO

        val (divisorExponent, divisorCoefficient) = other.terms.first()
        val divisorTail = fromTerms(*other.terms.drop(1).toTypedArray())

        val (dividendHead, remainingTerms) = this.terms.partition { it.exponent > divisorExponent }
        val dividendMatchingTerm = remainingTerms.find { it.exponent == divisorExponent }
        val dividendTail = fromTerms(*remainingTerms.filter { it.exponent < divisorExponent }.toTypedArray())

        val quotientHead = dividendHead.map { term ->
            Term(term.exponent - divisorExponent, term.coefficient)
        }

        val constantCoefficient = dividendMatchingTerm?.let { match ->
            val baseQuotient = match.coefficient / divisorCoefficient
            val needsBorrow = (match.coefficient % divisorCoefficient == 0) && (dividendTail < divisorTail)
            if (needsBorrow) baseQuotient - 1 else baseQuotient
        } ?: 0

        return fromTerms(*quotientHead.toTypedArray(), Term(ZERO, constantCoefficient))
    }

    operator fun div(other: Int): Ordinal {
        require(other > 0) { "Cannot divide an ordinal by a non-positive integer." }
        return this / fromInt(other)
    }

    operator fun rem(other: Ordinal): Ordinal {
        require(!other.isZero()) { "Cannot take modulus by zero ordinal." }
        if (other > this) return this
        return this - (other * (this / other))
    }

    operator fun rem(other: Int): Ordinal {
        require(other > 0) { "Cannot take modulus by non-positive integer." }
        return this % fromInt(other)
    }

    fun pow(exponent: Ordinal): Ordinal {
        if (exponent.isZero()) return ONE
        if (this.isZero()) return ZERO
        if (this == ONE) return ONE

        val baseTerm = this.terms.first()
        if (this.terms.size == 1 && baseTerm.exponent.isZero()) {
            val baseValue = baseTerm.coefficient
            val exponentTerm = exponent.terms.first()
            if (exponent.terms.size == 1 && exponentTerm.exponent.isZero()) {
                val exponentValue = exponentTerm.coefficient
                return fromInt(baseValue.pow(exponentValue))
            }
            val delta = exponent / OMEGA
            val finitePart = exponent % OMEGA
            val k = if (finitePart.isZero()) 0 else finitePart.terms.first().coefficient
            val omegaToTheDelta = fromTerms(Term(delta, 1))
            val baseToTheK = fromInt(baseValue.pow(k))
            return omegaToTheDelta * baseToTheK
        }
        val exponentLimitPartTerms = exponent.terms.filter { !it.exponent.isZero() }
        val exponentFinitePartTerm = exponent.terms.find { it.exponent.isZero() }
        val k = exponentFinitePartTerm?.coefficient ?: 0

        var baseToFinitePower = ONE
        for (i in 1..k) {
            baseToFinitePower *= this
        }

        if (exponentLimitPartTerms.isEmpty()) {
            return baseToFinitePower
        }
        val exponentLimitPart = fromTerms(exponentLimitPartTerms)
        val baseLeadingExponent = this.terms.first().exponent
        val resultLeadingExponent = baseLeadingExponent * exponentLimitPart
        val resultLimitPart = fromTerms(Term(resultLeadingExponent, 1))

        return resultLimitPart * baseToFinitePower
    }

    fun pow(other: Int): Ordinal {
        require(other >= 0) { "Exponent must be non-negative." }
        return pow(fromInt(other))
    }
}