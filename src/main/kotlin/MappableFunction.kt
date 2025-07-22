/**
 * 写像を表すクラス
 */
data class MappableFunction<A, B>(val mapping: Map<A, B>, val codom: Set<B>)
    : (A) -> B
{
    val dom: Set<A> get() = mapping.keys

    init {
        require(codom.containsAll(mapping.values))
    }

    override fun invoke(a: A): B {
        return mapping[a] ?: throw IllegalArgumentException()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MappableFunction<*, *>) return false
        return this.dom == other.dom
                && this.codom == other.codom
                && this.mapping == other.mapping
    }

    override fun hashCode(): Int {
        return mapping.hashCode()
    }

    override fun toString(): String {
        return "f${mapping}"
    }

    infix fun <C> composite(other: MappableFunction<C, A>)
            : MappableFunction<C, B> {
        require(other.codom == this.dom)
        val newMapping = other.mapping.mapValues { this(it.value) }
        return MappableFunction(newMapping, this.codom)
    }

    fun isInjective(): Boolean = mapping.values.toSet().size == mapping.size

    fun isSurjective(): Boolean = codom == mapping.values.toSet()

    fun isBijective(): Boolean = isInjective() && isSurjective()

    fun inverse(): MappableFunction<B, A> {
        require(isBijective())
        return MappableFunction(
            mapping.entries.associate { (key, value) -> value to key },
            dom
        )
    }

    fun graph(): Set<Pair<A, B>> =
        mapping.entries.map { it.key to it.value }.toSet()
}

fun <A, B> allMappings(dom: Set<A>, codom: Set<B>): Set<(A) -> B> =
    dom.fold(setOf(emptyMap<A, B>())) { a, k ->
        a.flatMap { existing ->
            codom.map { v ->
                existing + (k to v)
            }
        }.toSet()
    }.map { MappableFunction(it, codom) }.toSet()
