package ai.platon.pulsar.common.io

import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

interface Writable {
    @Throws(IOException::class)
    fun write(output: DataOutput)
    
    @Throws(IOException::class)
    fun readFields(input: DataInput)
}
