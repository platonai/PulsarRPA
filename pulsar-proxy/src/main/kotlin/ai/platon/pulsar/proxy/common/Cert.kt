package ai.platon.pulsar.proxy.common

import ai.platon.pulsar.proxy.server.HttpProxyServerConfig
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.*
import java.math.BigInteger
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.EncodedKeySpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import java.util.stream.IntStream

object CertPool {
    private val certCache: MutableMap<Int, MutableMap<String, X509Certificate?>> = WeakHashMap()
    @Throws(Exception::class)
    fun getCert(port: Int, host: String?, serverConfig: HttpProxyServerConfig): X509Certificate? {
        var cert: X509Certificate? = null
        if (host != null) {
            var portCertCache = certCache[port]
            if (portCertCache == null) {
                portCertCache = HashMap()
                certCache[port] = portCertCache
            }
            val key = host.trim { it <= ' ' }.toLowerCase()
            if (portCertCache.containsKey(key)) {
                return portCertCache[key]
            } else {
                cert = CertUtil.genCert(serverConfig.issuer, serverConfig.caPriKey,
                        serverConfig.caNotBefore, serverConfig.caNotAfter,
                        serverConfig.serverPubKey, key)
                portCertCache[key] = cert
            }
        }
        return cert
    }

    fun clear() {
        certCache.clear()
    }
}

object CertUtil {
    private var keyFactory: KeyFactory? = null
        get() {
            if (field == null) {
                field = KeyFactory.getInstance("RSA")
            }
            return field
        }

    /**
     * 生成RSA公私密钥对,长度为2048
     */
    @Throws(java.lang.Exception::class)
    fun genKeyPair(): KeyPair {
        val caKeyPairGen = KeyPairGenerator.getInstance("RSA", "BC")
        caKeyPairGen.initialize(2048, SecureRandom())
        return caKeyPairGen.genKeyPair()
    }

    /**
     * 从文件加载RSA私钥 openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out
     * ca_private.der
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun loadPriKey(bts: ByteArray?): PrivateKey {
        val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(bts)
        return keyFactory!!.generatePrivate(privateKeySpec)
    }

    /**
     * 从文件加载RSA私钥 openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out
     * ca_private.der
     */
    @Throws(java.lang.Exception::class)
    fun loadPriKey(path: String): PrivateKey {
        return loadPriKey(Files.readAllBytes(Paths.get(path)))
    }

    /**
     * 从文件加载RSA私钥 openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out
     * ca_private.der
     */
    @Throws(java.lang.Exception::class)
    fun loadPriKey(uri: URI): PrivateKey {
        return loadPriKey(Paths.get(uri).toString())
    }

    /**
     * 从文件加载RSA私钥 openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out
     * ca_private.der
     */
    @Throws(IOException::class, InvalidKeySpecException::class, NoSuchAlgorithmException::class)
    fun loadPriKey(inputStream: InputStream): PrivateKey {
        val outputStream = ByteArrayOutputStream()
        val bts = ByteArray(1024)
        var len: Int
        while (inputStream.read(bts).also { len = it } != -1) {
            outputStream.write(bts, 0, len)
        }
        inputStream.close()
        outputStream.close()
        return loadPriKey(outputStream.toByteArray())
    }

    /**
     * 从文件加载RSA公钥 openssl rsa -in ca.key -pubout -outform DER -out ca_pub.der
     */
    @Throws(java.lang.Exception::class)
    fun loadPubKey(bts: ByteArray?): PublicKey {
        val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(bts)
        return keyFactory!!.generatePublic(publicKeySpec)
    }

    /**
     * 从文件加载RSA公钥 openssl rsa -in ca.key -pubout -outform DER -out ca_pub.der
     */
    @Throws(java.lang.Exception::class)
    fun loadPubKey(path: String): PublicKey {
        val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(Files.readAllBytes(Paths.get(path)))
        return keyFactory!!.generatePublic(publicKeySpec)
    }

    /**
     * 从文件加载RSA公钥 openssl rsa -in ca.key -pubout -outform DER -out ca_pub.der
     */
    @Throws(java.lang.Exception::class)
    fun loadPubKey(uri: URI): PublicKey {
        return loadPubKey(Paths.get(uri).toString())
    }

    /**
     * 从文件加载RSA公钥 openssl rsa -in ca.key -pubout -outform DER -out ca_pub.der
     */
    @Throws(java.lang.Exception::class)
    fun loadPubKey(inputStream: InputStream): PublicKey {
        val outputStream = ByteArrayOutputStream()
        val bts = ByteArray(1024)
        var len: Int
        while (inputStream.read(bts).also { len = it } != -1) {
            outputStream.write(bts, 0, len)
        }
        inputStream.close()
        outputStream.close()
        return loadPubKey(outputStream.toByteArray())
    }

    /**
     * 从文件加载证书
     */
    @Throws(CertificateException::class)
    fun loadCert(inputStream: InputStream): X509Certificate {
        val cf = CertificateFactory.getInstance("X.509")
        return cf.generateCertificate(inputStream) as X509Certificate
    }

    /**
     * 从文件加载证书
     */
    @Throws(java.lang.Exception::class)
    fun loadCert(path: String): X509Certificate {
        return loadCert(FileInputStream(path))
    }

    /**
     * 从文件加载证书
     */
    @Throws(java.lang.Exception::class)
    fun loadCert(uri: URI): X509Certificate {
        return loadCert(Paths.get(uri).toString())
    }

    /**
     * 读取ssl证书使用者信息
     */
    @Throws(java.lang.Exception::class)
    fun getSubject(inputStream: InputStream): String {
        val certificate = loadCert(inputStream)
        //读出来顺序是反的需要反转下
        val tempList = Arrays.asList(*certificate.issuerDN.toString().split(", ").toTypedArray())
        return IntStream.rangeClosed(0, tempList.size - 1)
                .mapToObj { i: Int -> tempList[tempList.size - i - 1] }.collect(Collectors.joining(", "))
    }

    /**
     * 读取ssl证书使用者信息
     */
    @Throws(java.lang.Exception::class)
    fun getSubject(certificate: X509Certificate): String { //读出来顺序是反的需要反转下
        val tempList = Arrays.asList(*certificate.issuerDN.toString().split(", ").toTypedArray())
        return IntStream.rangeClosed(0, tempList.size - 1)
                .mapToObj { i: Int -> tempList[tempList.size - i - 1] }.collect(Collectors.joining(", "))
    }

    /**
     * 动态生成服务器证书,并进行CA签授
     *
     * @param issuer 颁发机构
     */
    @Throws(java.lang.Exception::class)
    fun genCert(issuer: String?, caPriKey: PrivateKey?, caNotBefore: Date?,
                caNotAfter: Date?, serverPubKey: PublicKey?,
                vararg hosts: String): X509Certificate { /* String issuer = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=ProxyeeRoot";
        String subject = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=" + host;*/
//根据CA证书subject来动态生成目标服务器证书的issuer和subject
        val subject = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=" + hosts[0]
        //doc from https://www.cryptoworkshop.com/guide/
        val jv3Builder = JcaX509v3CertificateBuilder(X500Name(issuer),  //issue#3 修复ElementaryOS上证书不安全问题(serialNumber为1时证书会提示不安全)，避免serialNumber冲突，采用时间戳+4位随机数生成
                BigInteger.valueOf(System.currentTimeMillis() + (Math.random() * 10000).toLong() + 1000),
                caNotBefore,
                caNotAfter,
                X500Name(subject),
                serverPubKey)
        //SAN扩展证书支持的域名，否则浏览器提示证书不安全
        val generalNames = arrayOfNulls<GeneralName>(hosts.size)
        for (i in hosts.indices) {
            generalNames[i] = GeneralName(GeneralName.dNSName, hosts[i])
        }
        val subjectAltName = GeneralNames(generalNames)
        jv3Builder.addExtension(Extension.subjectAlternativeName, false, subjectAltName)
        //SHA256 用SHA1浏览器可能会提示证书不安全
        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPriKey)
        return JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer))
    }

    /**
     * 生成CA服务器证书
     */
    @Throws(java.lang.Exception::class)
    fun genCACert(subject: String?, caNotBefore: Date?, caNotAfter: Date?,
                  keyPair: KeyPair): X509Certificate {
        val jv3Builder = JcaX509v3CertificateBuilder(X500Name(subject),
                BigInteger.valueOf(System.currentTimeMillis() + (Math.random() * 10000).toLong() + 1000),
                caNotBefore,
                caNotAfter,
                X500Name(subject),
                keyPair.public)
        jv3Builder.addExtension(Extension.basicConstraints, true, BasicConstraints(0))
        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .build(keyPair.private)
        return JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer))
    }

    init { //注册BouncyCastleProvider加密库
        Security.addProvider(BouncyCastleProvider())
    }
}

fun main() {
//生成ca证书和私钥
    val keyPair = CertUtil.genKeyPair()
    val caCertFile = File("e:/ssl/Proxyee.crt")
    if (caCertFile.exists()) {
        caCertFile.delete()
    }
    Files.write(Paths.get(caCertFile.toURI()),
            CertUtil.genCACert(
                    "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=Proxyee",
                    Date(),
                    Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3650)),
                    keyPair)
                    .encoded)
}
