package org.warps.pulsar.common;

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

public class FiledLines {

    private static final Logger LOG = LoggerFactory.getLogger(FiledLines.class);

    private Comparator<String> wordsComparator = null;

    private Map<String, TreeMultiset<String>> file2Lines = new HashMap<>();

    private Preprocessor preprocessor = new DefaultPreprocessor();

    public FiledLines() {
    }

    public FiledLines(String... files) {
        Validate.notNull(files);
        try {
            load(files);
        } catch (IOException e) {
            LOG.error("{}, files : {}", e, Arrays.asList(files));
        }
    }

    public FiledLines(Path... files) {
        Validate.notNull(files);
        try {
            load(files);
        } catch (IOException e) {
            LOG.error("{}, files : {}", e, Arrays.asList(files));
        }
    }

    public FiledLines(Comparator<String> wordsComparator, String... files) {
        this(files);
        this.wordsComparator = wordsComparator;
    }

    public Preprocessor getPreprocessor() {
        return preprocessor;
    }

    public void setPreprocessor(Preprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    public Multiset<String> getLines(String file) {
        if (file2Lines.isEmpty()) return TreeMultiset.create();

        return file2Lines.get(file);
    }

    public Multiset<String> getLines(Path path) {
        if (file2Lines.isEmpty()) return TreeMultiset.create();

        return file2Lines.get(path.toString());
    }

    public Multiset<String> firstFileLines() {
        if (file2Lines.isEmpty()) return TreeMultiset.create();

        return file2Lines.values().iterator().next();
    }

    public boolean add(String file, String text) {
        Multiset<String> lines = getLines(file);

        return lines != null && lines.add(text);
    }

    public boolean add(Path path, String text) {
        Multiset<String> lines = getLines(path.toString());
        return lines != null && lines.add(text);
    }

    public boolean addAll(String file, Collection<String> texts) {
        Multiset<String> lines = getLines(file);
        return lines != null && lines.addAll(texts);
    }

    public boolean addAll(Path path, Collection<String> texts) {
        Multiset<String> lines = getLines(path.toString());
        return lines != null && lines.addAll(texts);
    }

    public boolean remove(String file, String text) {
        Multiset<String> lines = getLines(file);

        return lines != null && lines.remove(text);
    }

    public boolean remove(Path path, String text) {
        Multiset<String> lines = getLines(path.toString());

        return lines != null && lines.remove(text);
    }

    public void clear() {
        file2Lines.clear();
    }

    public boolean contains(String file, String text) {
        Multiset<String> conf = getLines(file);
        return conf != null && conf.contains(text);

    }

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

    public void save(String file) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(file));

        for (String line : file2Lines.get(file).elementSet()) {
            pw.println(line);
        }

        pw.close();
    }

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
