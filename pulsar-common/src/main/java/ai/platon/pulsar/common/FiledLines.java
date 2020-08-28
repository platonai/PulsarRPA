package ai.platon.pulsar.common;

import com.google.common.base.Charsets;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import com.google.common.io.Files;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.*;

/**
 * <p>FiledLines class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class FiledLines {

    private static final Logger LOG = LoggerFactory.getLogger(FiledLines.class);

    private Comparator<String> wordsComparator = null;

    private Map<String, TreeMultiset<String>> file2Lines = new HashMap<>();

    private Preprocessor preprocessor = new DefaultPreprocessor();

    /**
     * <p>Constructor for FiledLines.</p>
     */
    public FiledLines() {
    }

    /**
     * <p>Constructor for FiledLines.</p>
     *
     * @param files a {@link java.lang.String} object.
     */
    public FiledLines(String... files) {
        Validate.notNull(files);
        try {
            load(files);
        } catch (IOException e) {
            LOG.error("{}, files : {}", e, Arrays.asList(files));
        }
    }

    /**
     * <p>Constructor for FiledLines.</p>
     *
     * @param files a {@link java.nio.file.Path} object.
     */
    public FiledLines(Path... files) {
        Validate.notNull(files);
        try {
            load(files);
        } catch (IOException e) {
            LOG.error("{}, files : {}", e, Arrays.asList(files));
        }
    }

    /**
     * <p>Constructor for FiledLines.</p>
     *
     * @param wordsComparator a {@link java.util.Comparator} object.
     * @param files a {@link java.lang.String} object.
     */
    public FiledLines(Comparator<String> wordsComparator, String... files) {
        this(files);
        this.wordsComparator = wordsComparator;
    }

    /**
     * <p>Getter for the field <code>preprocessor</code>.</p>
     *
     * @return a {@link ai.platon.pulsar.common.FiledLines.Preprocessor} object.
     */
    public Preprocessor getPreprocessor() {
        return preprocessor;
    }

    /**
     * <p>Setter for the field <code>preprocessor</code>.</p>
     *
     * @param preprocessor a {@link ai.platon.pulsar.common.FiledLines.Preprocessor} object.
     */
    public void setPreprocessor(Preprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    /**
     * <p>getLines.</p>
     *
     * @param file a {@link java.lang.String} object.
     * @return a {@link com.google.common.collect.Multiset} object.
     */
    public Multiset<String> getLines(String file) {
        if (file2Lines.isEmpty()) return TreeMultiset.create();

        return file2Lines.get(file);
    }

    /**
     * <p>getLines.</p>
     *
     * @param path a {@link java.nio.file.Path} object.
     * @return a {@link com.google.common.collect.Multiset} object.
     */
    public Multiset<String> getLines(Path path) {
        if (file2Lines.isEmpty()) return TreeMultiset.create();

        return file2Lines.get(path.toString());
    }

    /**
     * <p>firstFileLines.</p>
     *
     * @return a {@link com.google.common.collect.Multiset} object.
     */
    public Multiset<String> firstFileLines() {
        if (file2Lines.isEmpty()) return TreeMultiset.create();

        return file2Lines.values().iterator().next();
    }

    /**
     * <p>add.</p>
     *
     * @param file a {@link java.lang.String} object.
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean add(String file, String text) {
        Multiset<String> lines = getLines(file);

        return lines != null && lines.add(text);
    }

    /**
     * <p>add.</p>
     *
     * @param path a {@link java.nio.file.Path} object.
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean add(Path path, String text) {
        Multiset<String> lines = getLines(path.toString());
        return lines != null && lines.add(text);
    }

    /**
     * <p>addAll.</p>
     *
     * @param file a {@link java.lang.String} object.
     * @param texts a {@link java.util.Collection} object.
     * @return a boolean.
     */
    public boolean addAll(String file, Collection<String> texts) {
        Multiset<String> lines = getLines(file);
        return lines != null && lines.addAll(texts);
    }

    /**
     * <p>addAll.</p>
     *
     * @param path a {@link java.nio.file.Path} object.
     * @param texts a {@link java.util.Collection} object.
     * @return a boolean.
     */
    public boolean addAll(Path path, Collection<String> texts) {
        Multiset<String> lines = getLines(path.toString());
        return lines != null && lines.addAll(texts);
    }

    /**
     * <p>remove.</p>
     *
     * @param file a {@link java.lang.String} object.
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean remove(String file, String text) {
        Multiset<String> lines = getLines(file);

        return lines != null && lines.remove(text);
    }

    /**
     * <p>remove.</p>
     *
     * @param path a {@link java.nio.file.Path} object.
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean remove(Path path, String text) {
        Multiset<String> lines = getLines(path.toString());

        return lines != null && lines.remove(text);
    }

    /**
     * <p>clear.</p>
     */
    public void clear() {
        file2Lines.clear();
    }

    /**
     * <p>contains.</p>
     *
     * @param file a {@link java.lang.String} object.
     * @param text a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean contains(String file, String text) {
        Multiset<String> conf = getLines(file);
        return conf != null && conf.contains(text);

    }

    /**
     * <p>load.</p>
     *
     * @param paths a {@link java.nio.file.Path} object.
     * @throws java.io.IOException if any.
     */
    public void load(Path... paths) throws IOException {
        for (Path path : paths) {
            TreeMultiset<String> values = TreeMultiset.create(wordsComparator);
            List<String> lines = Files.readLines(path.toFile(), Charsets.UTF_8);

            for (String line : lines) {
                line = preprocessor.process(line);
                if (line != null && !line.isEmpty()) {
                    values.add(line);
                }
            }

            file2Lines.put(path.toString(), values);
        }
    }

    /**
     * <p>load.</p>
     *
     * @param files a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public void load(String... files) throws IOException {
        if (files.length == 0) {
            LOG.error("no file to load");
        }

        for (String file : files) {
            if (file != null && file.length() > 0) {
                TreeMultiset<String> values = TreeMultiset.create(wordsComparator);
                List<String> lines = Files.readLines(new File(file), Charsets.UTF_8);

                for (String line : lines) {
                    line = preprocessor.process(line);
                    if (line != null && !line.isEmpty()) values.add(line);
                }

                file2Lines.put(file, values);
            } else {
                LOG.error("bad file name");
            }
        }
    }

    /**
     * <p>save.</p>
     *
     * @param file a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public void save(String file) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(file));

        for (String line : file2Lines.get(file).elementSet()) {
            pw.println(line);
        }

        pw.close();
    }

    /**
     * <p>saveAll.</p>
     *
     * @throws java.io.IOException if any.
     */
    public void saveAll() throws IOException {
        for (String file : file2Lines.keySet()) {
            save(file);
        }
    }

    public interface Preprocessor {
        String process(String line);
    }

    public class DefaultPreprocessor implements Preprocessor {
        @Override
        public String process(String line) {
            return line.startsWith("#") ? "" : line.trim();
        }
    }
}
