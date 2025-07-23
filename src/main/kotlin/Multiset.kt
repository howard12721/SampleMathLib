interface Multiset<E> : Iterable<E> {
    val size: Int

    fun contains(element: E): Boolean

    fun add(element: E, occurrences: Int = 1)

    fun remove(element: E, occurrences: Int = 1): Boolean

    fun count(element: E): Int

    fun clear()
}

class MutableMultiset<E> : Multiset<E> {
    private val counts = mutableMapOf<E, Int>()

    override val size: Int
        get() = counts.values.sum()

    override fun contains(element: E): Boolean {
        return counts.containsKey(element)
    }

    override fun add(element: E, occurrences: Int) {
        require(occurrences > 0) { "Occurrences must be positive" }
        counts[element] = counts.getOrDefault(element, 0) + occurrences
    }

    override fun remove(element: E, occurrences: Int): Boolean {
        require(occurrences > 0) { "Occurrences must be positive" }

        val currentCount = counts.getOrDefault(element, 0)
        if (currentCount == 0) {
            return false
        }

        if (currentCount <= occurrences) {
            counts.remove(element)
        } else {
            counts[element] = currentCount - occurrences
        }
        return true
    }

    override fun count(element: E): Int {
        return counts.getOrDefault(element, 0)
    }

    override fun clear() {
        counts.clear()
    }

    override fun iterator(): Iterator<E> {
        val elements = mutableListOf<E>()
        counts.forEach { (element, count) ->
            repeat(count) {
                elements.add(element)
            }
        }
        return elements.iterator()
    }

    override fun toString(): String {
        return counts.entries
            .joinToString(prefix = "[", postfix = "]") { (element, count) ->
                "$element: $count"
            }
    }
}

fun <T> mutableMultisetOf(vararg elements: T) =
    MutableMultiset<T>().apply {
        elements.forEach(::add)
    }