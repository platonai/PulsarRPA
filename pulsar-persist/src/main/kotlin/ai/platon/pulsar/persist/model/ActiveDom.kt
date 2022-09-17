package ai.platon.pulsar.persist.model

import ai.platon.pulsar.persist.gora.generated.GActiveDOMStat
import ai.platon.pulsar.persist.gora.generated.GActiveDOMStatus
import com.google.gson.Gson

/**
 * Records the status of the DOM in a real browser.
 * */
data class ActiveDOMStatus(
        val n: Int = 0,
        val scroll: Int = 0,
        val st: String = "",
        val r: String = "",
        val idl: String = "",
        val ec: String = ""
)

/**
 * The statistics of the DOM in a real browser.
 * */
data class ActiveDOMStat(
        val ni: Int = 0,
        val na: Int = 0,
        val nnm: Int = 0,
        val nst: Int = 0,
        val w: Int = 0,
        val h: Int = 0
)

object Converters {
    fun convert(s: GActiveDOMStat): ActiveDOMStat {
        return ActiveDOMStat(s.ni, s.na, s.nnm, s.nst, s.w, s.h)
    }

    fun convert(s: ActiveDOMStat): GActiveDOMStat {
        return GActiveDOMStat().apply {
            ni = s.ni
            na = s.na
            nnm = s.nnm
            nst = s.nst
            w = s.w
            h = s.h
        }
    }

    fun convert(s: GActiveDOMStatus): ActiveDOMStatus {
        return ActiveDOMStatus(s.n, s.scroll, s.st.toString(), s.r.toString(), s.idl.toString(), s.ec.toString())
    }

    fun convert(s: ActiveDOMStatus): GActiveDOMStatus {
        return GActiveDOMStatus().apply {
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
data class ActiveDOMUrls(
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
        val default = ActiveDOMUrls()

        fun fromJson(json: String): ActiveDOMUrls {
            return gson.fromJson(json, ActiveDOMUrls::class.java)
        }
    }
}

data class ActiveDOMStatTrace(
    val status: ActiveDOMStatus? = ActiveDOMStatus(),
    val initStat: ActiveDOMStat? = ActiveDOMStat(),
    val lastStat: ActiveDOMStat? = ActiveDOMStat(),
    val initD: ActiveDOMStat? = ActiveDOMStat(),
    val lastD: ActiveDOMStat? = ActiveDOMStat()
) {
    override fun toString(): String {
        val s1 = initStat?:ActiveDOMStat()
        val s2 = lastStat?:ActiveDOMStat()
        val s3 = initD?:ActiveDOMStat()
        val s4 = lastD?:ActiveDOMStat()

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

        val st = status?:ActiveDOMStatus()
        return String.format("n:%s scroll:%s st:%s r:%s idl:%s\t%s\t(is,ls,id,ld)",
                st.n, st.scroll, st.st, st.r, st.idl, s)
    }

    fun toJson(): String{
        return gson.toJson(this)
    }

    companion object {
        private val gson = Gson()
        val default = ActiveDOMStatTrace()

        fun fromJson(json: String): ActiveDOMStatTrace {
            return gson.fromJson(json, ActiveDOMStatTrace::class.java)
        }
    }
}

data class ActiveDomMessage(
    var trace: ActiveDOMStatTrace? = null,
    var urls: ActiveDOMUrls? = null
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
