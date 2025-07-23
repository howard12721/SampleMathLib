/**
 * 写像を表すクラス
 */
data class MappableFunction<A, B>(val dom: Set<A>, val codom: Set<B>, val mapping: Map<A, B>)
    : (A) -> B
{

    init {
        require(dom == mapping.keys)
        require(mapping.values.all { it in codom })
    }

    constructor(codom: Set<B>, mapping: Map<A, B>)
            : this(mapping.keys, codom, mapping)

    constructor(dom: Set<A>, codom: Set<B>, mapping: (A) -> B)
            : this(dom, codom, dom.associateWith(mapping))

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
        return MappableFunction(this.codom, newMapping)
    }

    fun isInjective(): Boolean = mapping.values.toSet().size == mapping.size

    fun isSurjective(): Boolean = codom == mapping.values.toSet()

    fun isBijective(): Boolean = isInjective() && isSurjective()

    fun inverse(): MappableFunction<B, A> {
        require(isBijective())
        return MappableFunction(
            dom,
            mapping.entries.associate { (key, value) -> value to key }
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
    }.map { MappableFunction(codom, it) }.toSet()

fun <T> Set<T>.identityMapping(): MappableFunction<T, T> =
    MappableFunction(
        this,
        this
    ) { it }