package ai.platon.pulsar.common

import org.apache.commons.lang3.SystemUtils
import org.apache.commons.lang3.math.NumberUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.util.regex.Pattern

object Ping {

    fun ping(ipAddress: String): Boolean {
        val timeOut = 3000  //timeout is exprected to be more than 3s
        return InetAddress.getByName(ipAddress).isReachable(timeOut)
    }

    fun ping02(ipAddress: String) {
        try {
            val pro = Runtime.getRuntime().exec("ping $ipAddress")
            val buf = BufferedReader(InputStreamReader(pro.inputStream))
            var line = buf.readLine()
            while (line != null) {
                println(line)
                line = buf.readLine()
            }
        } catch (ex: Exception) {
            println(ex.message)
        }
    }

    fun ping(ipAddress: String, pingTimes: Int, timeOut: Int): Boolean {
        var reader: BufferedReader? = null
        val r = Runtime.getRuntime()  // 将要执行的ping命令,此命令是windows格式的命令

        val pingCommand = when {
            SystemUtils.IS_OS_WINDOWS -> "ping $ipAddress -n $pingTimes -w $timeOut"
            else -> "ping -c $pingTimes -w ${timeOut/1000} $ipAddress"
        }
        try {
            // 执行命令并获取输出
            // println(pingCommand)
            val p = r.exec(pingCommand) ?: return false
            reader = BufferedReader(InputStreamReader(p.inputStream))   // 逐行检查输出,计算类似出现=23ms TTL=62字样的次数
            var connectedCount = 0
            var line = reader.readLine()
            while (line != null) {
                println(line)

                connectedCount += getCheckResult(line)
                line = reader.readLine()
            } // 如果出现类似=23ms TTL=62这样的字样,出现的次数=测试次数则返回真
            return connectedCount == pingTimes
        } catch (ex: Exception) {
            ex.printStackTrace()   // 出现异常则返回假
            return false
        } finally {
            try {
                reader?.use { it.close() }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun pingNeedTime(ipAddress: String, pingTimes: Int, timeOut: Int): Float {
        var reader: BufferedReader? = null
        val time = 10000.0f
        val r = Runtime.getRuntime()
        val pingCommand = when {
            SystemUtils.IS_OS_WINDOWS -> "ping $ipAddress -n $pingTimes -w $timeOut"
            else -> "ping -c $pingTimes -w ${timeOut/1000} $ipAddress"
        }
        try {
            val p = r.exec(pingCommand) ?: return time
            reader = BufferedReader(InputStreamReader(p.inputStream))   // 逐行检查输出,计算类似出现=23ms TTL=62字样的次数
            var connectedCount = 0
            var timeTotal = 0.0f
            var spendTime = 0.0f
            var line = reader.readLine()
            while (line != null) {
                //System.out.println(line);
                spendTime = getTimes(line)
                if (spendTime > 0) {
                    timeTotal += spendTime
                    connectedCount++
                }
                line = reader.readLine()
            }

            return timeTotal / connectedCount
        } catch (ex: Exception) {
            ex.printStackTrace()   // 出现异常则返回假
            return time
        } finally {
            try {
                reader?.use { it.close() }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun getTimes(line: String): Float {
        // String cost = "来自 111.206.227.118 的回复: 字节=32 时间=40ms TTL=49";
        if (0 == getCheckResult(line)) {
            return 0.0f
        }

        val pattern = if (SystemUtils.IS_OS_WINDOWS) {
            Pattern.compile("(\\d+)ms")
        } else {
            Pattern.compile("(\\d+.?\\d+)\\s+ms")
        }

        val matcher = pattern.matcher(line)
        if (matcher.find()) {
            val str = matcher.group().substringBefore("ms").trim()
            return NumberUtils.toFloat(str)
        }

        return 10000.0f
    }

    //若line含有=18ms TTL=16字样,说明已经ping通,返回1,否則返回0.
    private fun getCheckResult(line: String): Int {
        val pattern = if (SystemUtils.IS_OS_WINDOWS) {
            Pattern.compile("(\\d+ms)(\\s+)(TTL=\\d+)", Pattern.CASE_INSENSITIVE)
        } else {
            Pattern.compile("(ttl=\\d+)(\\s+)(.+)=(\\d+.?\\d+ ms)", Pattern.CASE_INSENSITIVE)
        }

        val matcher = pattern.matcher(line)
        if (matcher.find()) {
            return 1
        }
        return 0
    }
}

fun main() {
    // val ipAddress = "127.0.0.1"
    val ipAddress = "www.baidu.com"
    // println(Ping.ping(ipAddress))
    // Ping.ping02(ipAddress)
    // println(Ping.ping(ipAddress, 5, 5000))
    val s = "64 bytes from localhost (180.97.33.107): icmp_seq=5 ttl=54 time=25.3 ms"
    val t = Ping.getTimes(s)
    println(t)
}
