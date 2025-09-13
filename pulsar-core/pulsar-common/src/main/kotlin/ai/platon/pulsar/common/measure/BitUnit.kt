package ai.platon.pulsar.common.measure

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
 * @author Fabian Barney
 */
enum class BitUnit {
    BIT {
        override fun toBits(d: Double): Double {
            return d
        }

        override fun convert(d: Double, u: BitUnit): Double {
            return u.toBits(d)
        }
    },
    KIBIT {
        override fun toBits(d: Double): Double {
            return safeMulti(d, C_KIBIT)
        }

        override fun convert(d: Double, u: BitUnit): Double {
            return u.toKibit(d)
        }
    },
    MIBIT {
        override fun toBits(d: Double): Double {
            return safeMulti(d, C_MIBIT)
        }

        override fun convert(d: Double, u: BitUnit): Double {
            return u.toMibit(d)
        }
    },
    GIBIT {
        override fun toBits(d: Double): Double {
            return safeMulti(d, C_GIBIT)
        }

        override fun convert(d: Double, u: BitUnit): Double {
            return u.toGibit(d)
        }
    },
    TIBIT {
        override fun toBits(d: Double): Double {
            return safeMulti(d, C_TIBIT)
        }

        override fun convert(d: Double, u: BitUnit): Double {
            return u.toTibit(d)
        }
    },
    PIBIT {
        override fun toBits(d: Double): Double {
            return safeMulti(d, C_PIBIT)
        }

        override fun convert(d: Double, u: BitUnit): Double {
            return u.toPibit(d)
        }
    },
    KBIT {
        override fun toBits(d: Double): Double {
            return safeMulti(d, C_KBIT)
        }

        override fun convert(d: Double, u: BitUnit): Double {
            return u.toKbit(d)
        }
    },
    MBIT {
        override fun toBits(d: Double): Double {
            return safeMulti(d, C_MBIT)
        }

        override fun convert(d: Double, u: BitUnit): Double {
            return u.toMbit(d)
        }
    },
    GBIT {
        override fun toBits(d: Double): Double {
            return safeMulti(d, C_GBIT)
        }

        override fun convert(d: Double, u: BitUnit): Double {
            return u.toGbit(d)
        }
    },
    TBIT {
        override fun toBits(d: Double): Double {
            return safeMulti(d, C_TBIT)
        }

        override fun convert(d: Double, u: BitUnit): Double {
            return u.toTbit(d)
        }
    },
    PBIT {
        override fun toBits(d: Double): Double {
            return safeMulti(d, C_PBIT)
        }

        override fun convert(d: Double, u: BitUnit): Double {
            return u.toPbit(d)
        }
    };

    abstract fun toBits(d: Double): Double
    fun toKibit(d: Double): Double {
        return toBits(d) / C_KIBIT
    }

    fun toMibit(d: Double): Double {
        return toBits(d) / C_MIBIT
    }

    fun toGibit(d: Double): Double {
        return toBits(d) / C_GIBIT
    }

    fun toTibit(d: Double): Double {
        return toBits(d) / C_TIBIT
    }

    fun toPibit(d: Double): Double {
        return toBits(d) / C_PIBIT
    }

    fun toKbit(d: Double): Double {
        return toBits(d) / C_KBIT
    }

    fun toMbit(d: Double): Double {
        return toBits(d) / C_MBIT
    }

    fun toGbit(d: Double): Double {
        return toBits(d) / C_GBIT
    }

    fun toTbit(d: Double): Double {
        return toBits(d) / C_TBIT
    }

    fun toPbit(d: Double): Double {
        return toBits(d) / C_PBIT
    }

    abstract fun convert(d: Double, u: BitUnit): Double
    @JvmOverloads
    fun convert(d: Double, u: ByteUnit, wordSize: Int = java.lang.Byte.SIZE): Double {
        val bits = safeMulti(u.toBytes(d), wordSize.toDouble())
        return convert(bits, BIT)
    }

    /*
     * Komfort-Methoden f�r Cross-Konvertierung
     */
    fun toBytes(d: Double): Double {
        return ByteUnit.BYTE.convert(d, this)
    }

    fun toBytes(d: Double, wordSize: Int): Double {
        return ByteUnit.BYTE.convert(d, this, wordSize)
    }

    fun toKiB(d: Double): Double {
        return ByteUnit.KIB.convert(d, this)
    }

    fun toMiB(d: Double): Double {
        return ByteUnit.MIB.convert(d, this)
    }

    fun toGiB(d: Double): Double {
        return ByteUnit.GIB.convert(d, this)
    }

    fun toTiB(d: Double): Double {
        return ByteUnit.TIB.convert(d, this)
    }

    fun toPiB(d: Double): Double {
        return ByteUnit.PIB.convert(d, this)
    }

    fun toKiB(d: Double, wordSize: Int): Double {
        return ByteUnit.KIB.convert(d, this, wordSize)
    }

    fun toMiB(d: Double, wordSize: Int): Double {
        return ByteUnit.MIB.convert(d, this, wordSize)
    }

    fun toGiB(d: Double, wordSize: Int): Double {
        return ByteUnit.GIB.convert(d, this, wordSize)
    }

    fun toTiB(d: Double, wordSize: Int): Double {
        return ByteUnit.TIB.convert(d, this, wordSize)
    }

    fun toPiB(d: Double, wordSize: Int): Double {
        return ByteUnit.PIB.convert(d, this, wordSize)
    }

    fun toKB(d: Double): Double {
        return ByteUnit.KB.convert(d, this)
    }

    fun toMB(d: Double): Double {
        return ByteUnit.MB.convert(d, this)
    }

    fun toGB(d: Double): Double {
        return ByteUnit.GB.convert(d, this)
    }

    fun toTB(d: Double): Double {
        return ByteUnit.TB.convert(d, this)
    }

    fun toPB(d: Double): Double {
        return ByteUnit.PB.convert(d, this)
    }

    fun toKB(d: Double, wordSize: Int): Double {
        return ByteUnit.KB.convert(d, this, wordSize)
    }

    fun toMB(d: Double, wordSize: Int): Double {
        return ByteUnit.MB.convert(d, this, wordSize)
    }

    fun toGB(d: Double, wordSize: Int): Double {
        return ByteUnit.GB.convert(d, this, wordSize)
    }

    fun toTB(d: Double, wordSize: Int): Double {
        return ByteUnit.TB.convert(d, this, wordSize)
    }

    fun toPB(d: Double, wordSize: Int): Double {
        return ByteUnit.PB.convert(d, this, wordSize)
    }

    companion object {
        val C_KIBIT = Math.pow(2.0, 10.0)
        val C_MIBIT = Math.pow(2.0, 20.0)
        val C_GIBIT = Math.pow(2.0, 30.0)
        val C_TIBIT = Math.pow(2.0, 40.0)
        val C_PIBIT = Math.pow(2.0, 50.0)
        val C_KBIT = Math.pow(10.0, 3.0)
        val C_MBIT = Math.pow(10.0, 6.0)
        val C_GBIT = Math.pow(10.0, 9.0)
        val C_TBIT = Math.pow(10.0, 12.0)
        val C_PBIT = Math.pow(10.0, 15.0)

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