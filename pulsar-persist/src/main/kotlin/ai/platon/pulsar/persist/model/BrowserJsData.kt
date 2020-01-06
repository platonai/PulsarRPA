package ai.platon.pulsar.persist.model

import com.google.gson.Gson

data class BrowserJsData(
        val status: Status = Status(),
        val initStat: Stat = Stat(),
        val lastStat: Stat = Stat(),
        val initD: Stat = Stat(),
        val lastD: Stat = Stat(),
        var urls: Urls = Urls()
) {
    data class Status(
            val n: Int = 0,
            val scroll: Int = 0,
            val st: String = "",
            val r: String = "",
            val idl: String = ""
    )

    data class Stat(
            val ni: Int = 0,
            val na: Int = 0,
            val nnm: Int = 0,
            val nst: Int = 0,
            val w: Int = 0,
            val h: Int = 0
    )

    // NOTE: it seems they are all the same
    data class Urls(
            val URL: String = "",
            val baseURI: String = "",
            val location: String = "",
            val documentURI: String = ""
    )

    override fun toString(): String {
        val s1 = initStat
        val s2 = lastStat
        val s3 = initD
        val s4 = lastD

        val s = String.format(
                "img: %s/%s/%s/%s, a: %s/%s/%s/%s, num: %s/%s/%s/%s, st: %s/%s/%s/%s, " +
                        "w: %s/%s/%s/%s, h: %s/%s/%s/%s",
                s1.ni,  s2.ni,  s3.ni,  s4.ni,
                s1.na,  s2.na,  s3.na,  s4.na,
                s1.nnm, s2.nnm, s3.nnm, s4.nnm,
                s1.nst, s2.nst, s3.nst, s4.nst,
                s1.w,   s2.w,   s3.w,   s4.w,
                s1.h,   s2.h,   s3.h,   s4.h
        )

        val st = status
        return String.format("n:%s scroll:%s st:%s r:%s idl:%s\t%s\t(is,ls,id,ld)",
                st.n, st.scroll, st.st, st.r, st.idl, s)
    }

    fun toJson(): String{
        return browserDataGson.toJson(this)
    }

    companion object {
        private val browserDataGson = Gson()
        val default = BrowserJsData()

        fun fromJson(json: String): BrowserJsData {
            return browserDataGson.fromJson(json, BrowserJsData::class.java)
        }
    }
}
