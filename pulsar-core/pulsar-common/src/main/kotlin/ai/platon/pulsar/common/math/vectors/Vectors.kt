package ai.platon.pulsar.common.math.vectors

import org.apache.commons.math3.linear.RealVector

operator fun RealVector.set(index: Int, value: Double) { setEntry(index, value) }

operator fun RealVector.get(index: Int): Double { return getEntry(index) }

val RealVector.isEmpty: Boolean get() = dimension == 0

val RealVector.isNotEmpty: Boolean get() = !isEmpty
