package ai.platon.pulsar.persist.model

import ai.platon.pulsar.persist.gora.generated.GActiveDomStat
import ai.platon.pulsar.persist.gora.generated.GActiveDomStatus
import com.google.gson.Gson

/**
 * An active DOM is a DOM inside a real browser
 * */
data class ActiveDomStatus(
        val n: Int = 0,
        val scroll: Int = 0,
        val st: String = "",
        val r: String = "",
        val idl: String = "",
        val ec: String = ""
)

data class ActiveDomStat(
        val ni: Int = 0,
        val na: Int = 0,
        val nnm: Int = 0,
        val nst: Int = 0,
        val w: Int = 0,
        val h: Int = 0
)

object Converters {
    fun convert(s: GActiveDomStat): ActiveDomStat {
        return ActiveDomStat(s.ni, s.na, s.nnm, s.nst, s.w, s.h)
    }

    fun convert(s: ActiveDomStat): GActiveDomStat {
        return GActiveDomStat().apply {
            ni = s.ni
            na = s.na
            nnm = s.nnm
            nst = s.nst
            w = s.w
            h = s.h
        }
    }

    fun convert(s: GActiveDomStatus): ActiveDomStatus {
        return ActiveDomStatus(s.n, s.scroll, s.st.toString(), s.r.toString(), s.idl.toString(), s.ec.toString())
    }

    fun convert(s: ActiveDomStatus): GActiveDomStatus {
        return GActiveDomStatus().apply {
            n = s.n
            st = s.st
            r = s.r
            idl = s.idl
            ec = s.ec
        }
    }
}

/**
 * URLs of a document.
 * */
data class ActiveDomUrls(
    /**
     * The entire URL of the document, including the protocol (like http://)
     */
    var URL: String = "",
    /**
     * The baseURI is a property of Node, it's the absolute base URL of the
     * document containing the node. A baseURI is used to resolve relative URLs.
     *
     * The base URL is determined as follows:
     * 1. By default, the base URL is the location of the document
     *    (as determined by window.location).
     * 2. If the document has an `<base>` element, its href attribute is used.
     * */
    var baseURI: String = "",
    /**
     * The `window.location`, or `document.location`, is a read-only property
     * returns a Location object, which contains information about the URL of the
     * document and provides methods for changing that URL and loading another URL.
     *
     * To retrieve just the URL as a string, the read-only `document.URL` property can
     * also be used.
     * */
    var location: String = "",
    /**
     * Returns the document location as a string.
     *
     * The documentURI property can be used on any document types. The document.URL
     * property can only be used on HTML documents.
     *
     * @see https://www.w3schools.com/jsref/prop_document_documenturi.asp
     * */
    var documentURI: String = "",
    /** Returns the URI of the page that linked to this page. */
    var referrer: String = "",
) {
    fun toJson(): String{
        return gson.toJson(this)
    }

    companion object {
        private val gson = Gson()
        val default = ActiveDomUrls()

        fun fromJson(json: String): ActiveDomUrls {
            return gson.fromJson(json, ActiveDomUrls::class.java)
        }
    }
}

data class ActiveDomMultiStatus(
        val status: ActiveDomStatus? = ActiveDomStatus(),
        val initStat: ActiveDomStat? = ActiveDomStat(),
        val lastStat: ActiveDomStat? = ActiveDomStat(),
        val initD: ActiveDomStat? = ActiveDomStat(),
        val lastD: ActiveDomStat? = ActiveDomStat()
) {
    override fun toString(): String {
        val s1 = initStat?:ActiveDomStat()
        val s2 = lastStat?:ActiveDomStat()
        val s3 = initD?:ActiveDomStat()
        val s4 = lastD?:ActiveDomStat()

        val s = String.format(
                "img: %s/%s/%s/%s, a: %s/%s/%s/%s, num: %s/%s/%s/%s, st: %s/%s/%s/%s, " +
                        "w: %s/%s/%s/%s, h: %s/%s/%s/%s",
                s1.ni, s2.ni, s3.ni, s4.ni,
                s1.na, s2.na, s3.na, s4.na,
                s1.nnm, s2.nnm, s3.nnm, s4.nnm,
                s1.nst, s2.nst, s3.nst, s4.nst,
                s1.w, s2.w, s3.w, s4.w,
                s1.h, s2.h, s3.h, s4.h
        )

        val st = status?:ActiveDomStatus()
        return String.format("n:%s scroll:%s st:%s r:%s idl:%s\t%s\t(is,ls,id,ld)",
                st.n, st.scroll, st.st, st.r, st.idl, s)
    }

    fun toJson(): String{
        return gson.toJson(this)
    }

    companion object {
        private val gson = Gson()
        val default = ActiveDomMultiStatus()

        fun fromJson(json: String): ActiveDomMultiStatus {
            return gson.fromJson(json, ActiveDomMultiStatus::class.java)
        }
    }
}

data class ActiveDomMessage(
        var multiStatus: ActiveDomMultiStatus? = null,
        var urls: ActiveDomUrls? = null
) {
    fun toJson(): String {
        return gson.toJson(this)
    }

    companion object {
        private val gson = Gson()
        val default = ActiveDomMessage()

        fun fromJson(json: String): ActiveDomMessage {
            return gson.fromJson(json, ActiveDomMessage::class.java)
        }
    }
}
