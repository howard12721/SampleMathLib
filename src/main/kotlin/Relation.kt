/**
 * 二項関係を表すクラス
 */
data class BinaryRelation<A, B>(
    val l: Set<A>,
    val r: Set<B>,
    val rel: Set<Pair<A, B>>
) {
    init {
        require(rel.all { it.first in l && it.second in r })
    }

    constructor(l: Set<A>, r: Set<B>, condition: (A, B) -> Boolean) : this(
        l,
        r,
        l.flatMap { first -> r.map { second -> Pair(first, second) } }
            .filter { condition(it.first, it.second) }
            .toSet()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryRelation<*, *>) return false
        return this.l == other.l
                && this.r == other.r
                && this.rel == other.rel
    }

    override fun hashCode(): Int {
        return 31 * l.hashCode() + 31 * r.hashCode() + rel.hashCode()
    }

    infix fun <C> composite(other: BinaryRelation<C, A>)
            : BinaryRelation<C, B> {
        require(other.r == this.l)
        return BinaryRelation(
            other.l,
            this.r,
            other.rel.flatMap { (c, a) ->
                this.rel.filter { it.first == a }.map { Pair(c, it.second) }
            }.toSet()
        )
    }
}

/**
 * ある集合上の関係を表すクラス
 */
data class Endorelation<A>(val a: Set<A>, val rel: Set<Pair<A, A>>) {
    init {
        require(rel.all { it.first in a && it.second in a })
    }

    constructor(a: Set<A>, condition: (A, A) -> Boolean) : this(
        a,
        a.flatMap { first -> a.map { second -> Pair(first, second) } }
            .filter { condition(it.first, it.second) }
            .toSet()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Endorelation<*>) return false
        return this.a == other.a && this.rel == other.rel
    }

    override fun hashCode(): Int {
        return 31 * a.hashCode() + rel.hashCode()
    }

    infix fun composite(other: Endorelation<A>): Endorelation<A> =
        Endorelation(
            this.a,
            other.rel.flatMap { (l, r) ->
                this.rel.filter { it.first == r }.map { Pair(l, it.second) }
            }.toSet()
        )

    fun power(amount: Int): Endorelation<A> {
        require(amount >= 0) { "Amount must be non-negative" }
        return if (amount == 0) {
            Endorelation(a, a.map { it to it }.toSet())
        } else {
            (1 until amount).fold(this) { acc, _ -> acc composite this }
        }
    }

    fun transitiveClosure(): Endorelation<A> {
        val closureRel = this.rel.toMutableSet()
        for (k in this.a) {
            for (i in this.a) {
                for (j in this.a) {
                    if (closureRel.contains(i to k)
                        && closureRel.contains(k to j)) {
                        closureRel.add(i to j)
                    }
                }
            }
        }
        return Endorelation(this.a, closureRel)
    }

    fun reflectiveTransitiveClosure(): Endorelation<A> {
        return Endorelation(
            this.a,
            transitiveClosure().rel.toMutableSet()
                .apply { addAll(power(0).rel) }
        )
    }

    fun isReflective(): Boolean = this.a.all { it to it in this.rel }

    fun isSymmetric(): Boolean =
        this.rel.all { (a, b) -> (b to a) in this.rel }

    fun isAntisymmetric(): Boolean = this.rel.all { (a, b) ->
        a == b || (b to a) !in this.rel
    }

    fun isTransitive(): Boolean = this.rel.all { (a, b) ->
        this.rel.all { (c, d) -> b != c || (a to d) in this.rel }
    }

    fun isEquivalence(): Boolean =
        isReflective() && isSymmetric() && isTransitive()

    fun getEquivalenceClass(representative: A): Set<A> {
        require(isEquivalence())
        require(representative in a)

        return this.rel
            .filter { it.first == representative }
            .map { it.second }
            .toSet()
    }

    fun getQuotientSet(): Set<Set<A>> {
        require(isEquivalence())

        val groups = this.a.fold(mutableMapOf<A, MutableSet<A>>()) { acc, element ->
            val representative = acc.keys.find { rep -> element to rep in this.rel }

            if (representative != null) {
                acc[representative]?.add(element)
            } else {
                acc[element] = mutableSetOf(element)
            }
            acc
        }

        return groups.values.toSet()
    }

    fun isPartialOrder(): Boolean =
        isReflective() && isAntisymmetric() && isTransitive()

    fun isTotalOrder(): Boolean =
        isPartialOrder() && this.a.all { x ->
            this.a.all { y ->
                (x to y) in this.rel || (y to x) in this.rel
            }
        }
}
