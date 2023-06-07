import java.io.*
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

class JarTool {
    private val manifest: Manifest = Manifest()

    fun startManifest() {
        manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
    }

    fun setMainClass(mainFQCN: String?) {
        if (mainFQCN != null && mainFQCN != "") {
            manifest.mainAttributes[Attributes.Name.MAIN_CLASS] = mainFQCN
        }
    }

    fun addToManifest(key: String?, value: String?) {
        manifest.mainAttributes[Attributes.Name(key)] = value
    }

    @Throws(IOException::class)
    fun openJar(jarFile: String): JarOutputStream {
        return JarOutputStream(FileOutputStream(jarFile), manifest)
    }

    @Throws(FileNotFoundException::class, IOException::class)
    fun addFile(target: JarOutputStream, rootPath: String, source: String) {
        val remaining = if (rootPath.endsWith(File.separator)) {
            source.substring(rootPath.length)
        } else {
            source.substring(rootPath.length + 1)
        }

        val name = remaining.replace("\\", "/")
        val entry = JarEntry(name)
        entry.time = File(source).lastModified()
        target.putNextEntry(entry)
        val bis = BufferedInputStream(FileInputStream(source))
        val buffer = ByteArray(1024)
        while (true) {
            val count = bis.read(buffer)
            if (count == -1) {
                break
            }
            target.write(buffer, 0, count)
        }
        target.closeEntry()
        bis.close()
    }
}
