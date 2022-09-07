package ai.platon.pulsar.common.measure

import ai.platon.pulsar.common.measure.BitUnit

/*
* Copyright 2011 Fabian Barney
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/ /**
 *
 * @author Fabian Barney
 */
enum class ByteUnit {
    /** <pre>
     * Byte (B)
     * 1 Byte
    </pre> */
    BYTE {
        override fun toBytes(d: Double): Double {
            return d
        }

        override fun convert(d: Double, u: ByteUnit): Double {
            return u.toBytes(d)
        }
    },

    /** <pre>
     * Kibibyte (KiB)
     * 2^10 Byte = 1.024 Byte
    </pre> */
    KIB {
        override fun toBytes(d: Double): Double {
            return safeMulti(d, C_KIB)
        }

        override fun convert(d: Double, u: ByteUnit): Double {
            return u.toKiB(d)
        }
    },

    /** <pre>
     * Mebibyte (MiB)
     * 2^20 Byte = 1.024 * 1.024 Byte = 1.048.576 Byte
    </pre> */
    MIB {
        override fun toBytes(d: Double): Double {
            return safeMulti(d, C_MIB)
        }

        override fun convert(d: Double, u: ByteUnit): Double {
            return u.toMiB(d)
        }
    },

    /** <pre>
     * Gibibyte (GiB)
     * 2^30 Byte = 1.024 * 1.024 * 1.024 Byte = 1.073.741.824 Byte
    </pre> */
    GIB {
        override fun toBytes(d: Double): Double {
            return safeMulti(d, C_GIB)
        }

        override fun convert(d: Double, u: ByteUnit): Double {
            return u.toGiB(d)
        }
    },

    /** <pre>
     * Tebibyte (TiB)
     * 2^40 Byte = 1.024 * 1.024 * 1.024 * 1.024 Byte = 1.099.511.627.776 Byte
    </pre> */
    TIB {
        override fun toBytes(d: Double): Double {
            return safeMulti(d, C_TIB)
        }

        override fun convert(d: Double, u: ByteUnit): Double {
            return u.toTiB(d)
        }
    },

    /** <pre>
     * Pebibyte (PiB)
     * 2^50 Byte = 1.024 * 1.024 * 1.024 * 1.024 * 1.024 Byte = 1.125.899.906.842.624 Byte
    </pre> */
    PIB {
        override fun toBytes(d: Double): Double {
            return safeMulti(d, C_PIB)
        }

        override fun convert(d: Double, u: ByteUnit): Double {
            return u.toPiB(d)
        }
    },

    /** <pre>
     * Kilobyte (kB)
     * 10^3 Byte = 1.000 Byte
    </pre> */
    KB {
        override fun toBytes(d: Double): Double {
            return safeMulti(d, C_KB)
        }

        override fun convert(d: Double, u: ByteUnit): Double {
            return u.toKB(d)
        }
    },

    /** <pre>
     * Megabyte (MB)
     * 10^6 Byte = 1.000.000 Byte
    </pre> */
    MB {
        override fun toBytes(d: Double): Double {
            return safeMulti(d, C_MB)
        }

        override fun convert(d: Double, u: ByteUnit): Double {
            return u.toMB(d)
        }
    },

    /** <pre>
     * Gigabyte (GB)
     * 10^9 Byte = 1.000.000.000 Byte
    </pre> */
    GB {
        override fun toBytes(d: Double): Double {
            return safeMulti(d, C_GB)
        }

        override fun convert(d: Double, u: ByteUnit): Double {
            return u.toGB(d)
        }
    },

    /** <pre>
     * Terabyte (TB)
     * 10^12 Byte = 1.000.000.000.000 Byte
    </pre> */
    TB {
        override fun toBytes(d: Double): Double {
            return safeMulti(d, C_TB)
        }

        override fun convert(d: Double, u: ByteUnit): Double {
            return u.toTB(d)
        }
    },

    /** <pre>
     * Petabyte (PB)
     * 10^15 Byte = 1.000.000.000.000.000 Byte
    </pre> */
    PB {
        override fun toBytes(d: Double): Double {
            return safeMulti(d, C_PB)
        }

        override fun convert(d: Double, u: ByteUnit): Double {
            return u.toPB(d)
        }
    };

    abstract fun toBytes(d: Double): Double

    fun toKiB(d: Double) = toBytes(d) / C_KIB

    fun toKiB(d: Long) = toKiB(d.toDouble())

    fun toMiB(d: Double) = toBytes(d) / C_MIB

    fun toMiB(d: Long) = toMiB(d.toDouble())

    fun toGiB(d: Double) = toBytes(d) / C_GIB

    fun toGiB(d: Long) = toGiB(d.toDouble())

    fun toTiB(d: Double) = toBytes(d) / C_TIB

    fun toTiB(d: Long) = toTiB(d.toDouble())

    fun toPiB(d: Double) = toBytes(d) / C_PIB

    fun toPiB(d: Long) = toPiB(d.toDouble())

    fun toKB(d: Double) = toBytes(d) / C_KB

    fun toKB(d: Long) = toKB(d.toDouble())

    fun toMB(d: Double) = toBytes(d) / C_MB

    fun toMB(d: Long) = toMB(d.toDouble())

    fun toGB(d: Double) = toBytes(d) / C_GB

    fun toGB(d: Long) = toGB(d.toDouble())

    fun toTB(d: Double) = toBytes(d) / C_TB

    fun toTB(d: Long) = toTB(d.toDouble())

    fun toPB(d: Double) = toBytes(d) / C_PB

    fun toPB(d: Long) = toPB(d.toDouble())

    abstract fun convert(d: Double, u: ByteUnit): Double

    @JvmOverloads
    fun convert(d: Double, u: BitUnit, wordSize: Int = java.lang.Byte.SIZE): Double {
        val bytes = u.toBits(d) / wordSize
        return convert(bytes, BYTE)
    }

    /*
     * Komfort-Methoden for Cross-Konvertierung
     */
    fun toBits(d: Double): Double {
        return BitUnit.BIT.convert(d, this)
    }

    fun toBits(d: Double, wordSize: Int): Double {
        return BitUnit.BIT.convert(d, this, wordSize)
    }

    fun toKibit(d: Double): Double {
        return BitUnit.KIBIT.convert(d, this)
    }

    fun toMibit(d: Double): Double {
        return BitUnit.MIBIT.convert(d, this)
    }

    fun toGibit(d: Double): Double {
        return BitUnit.GIBIT.convert(d, this)
    }

    fun toTibit(d: Double): Double {
        return BitUnit.TIBIT.convert(d, this)
    }

    fun toPibit(d: Double): Double {
        return BitUnit.PIBIT.convert(d, this)
    }

    fun toKibit(d: Double, wordSize: Int): Double {
        return BitUnit.KIBIT.convert(d, this, wordSize)
    }

    fun toMibit(d: Double, wordSize: Int): Double {
        return BitUnit.MIBIT.convert(d, this, wordSize)
    }

    fun toGibit(d: Double, wordSize: Int): Double {
        return BitUnit.GIBIT.convert(d, this, wordSize)
    }

    fun toTibit(d: Double, wordSize: Int): Double {
        return BitUnit.TIBIT.convert(d, this, wordSize)
    }

    fun toPibit(d: Double, wordSize: Int): Double {
        return BitUnit.PIBIT.convert(d, this, wordSize)
    }

    fun toKbit(d: Double): Double {
        return BitUnit.KBIT.convert(d, this)
    }

    fun toMbit(d: Double): Double {
        return BitUnit.MBIT.convert(d, this)
    }

    fun toGbit(d: Double): Double {
        return BitUnit.GBIT.convert(d, this)
    }

    fun toTbit(d: Double): Double {
        return BitUnit.TBIT.convert(d, this)
    }

    fun toPbit(d: Double): Double {
        return BitUnit.PBIT.convert(d, this)
    }

    fun toKbit(d: Double, wordSize: Int): Double {
        return BitUnit.KBIT.convert(d, this, wordSize)
    }

    fun toMbit(d: Double, wordSize: Int): Double {
        return BitUnit.MBIT.convert(d, this, wordSize)
    }

    fun toGbit(d: Double, wordSize: Int): Double {
        return BitUnit.GBIT.convert(d, this, wordSize)
    }

    fun toTbit(d: Double, wordSize: Int): Double {
        return BitUnit.TBIT.convert(d, this, wordSize)
    }

    fun toPbit(d: Double, wordSize: Int): Double {
        return BitUnit.PBIT.convert(d, this, wordSize)
    }

    companion object {
        val C_KIB = Math.pow(2.0, 10.0)
        val C_MIB = Math.pow(2.0, 20.0)
        val C_GIB = Math.pow(2.0, 30.0)
        val C_TIB = Math.pow(2.0, 40.0)
        val C_PIB = Math.pow(2.0, 50.0)
        val C_KB = Math.pow(10.0, 3.0)
        val C_MB = Math.pow(10.0, 6.0)
        val C_GB = Math.pow(10.0, 9.0)
        val C_TB = Math.pow(10.0, 12.0)
        val C_PB = Math.pow(10.0, 15.0)

        private const val MAX = Double.MAX_VALUE

        fun safeMulti(d: Double, multi: Double): Double {
            val limit = MAX / multi
            if (d > limit) {
                return Double.MAX_VALUE
            }
            return if (d < -limit) {
                Double.MIN_VALUE
            } else d * multi
        }
    }
}
