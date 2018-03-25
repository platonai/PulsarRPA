package fun.platonic.pulsar.persist;

import fun.platonic.pulsar.persist.gora.generated.GFieldGroup;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vincent on 17-8-2.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class FieldGroupFormatter {
    String name;
    String title;
    String author;
    List<String> authors;
    List<String> directors;
    String created;
    String content;
    String lastModified;
    int vote;
    int review;
    int forward;
    int oppose;
    String sourceLink;
    String sourceTitle;
    private GFieldGroup fieldGroup;
    private long id;

    public FieldGroupFormatter(GFieldGroup fieldGroup) {
        this.fieldGroup = fieldGroup;
    }

    public FieldGroupFormatter(FieldGroup fieldGroup) {
        this.fieldGroup = fieldGroup.unbox();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public List<String> getDirectors() {
        return directors;
    }

    public void setDirectors(List<String> directors) {
        this.directors = directors;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public int getVote() {
        return vote;
    }

    public void setVote(int vote) {
        this.vote = vote;
    }

    public int getReview() {
        return review;
    }

    public void setReview(int review) {
        this.review = review;
    }

    public int getForward() {
        return forward;
    }

    public void setForward(int forward) {
        this.forward = forward;
    }

    public int getOppose() {
        return oppose;
    }

    public void setOppose(int oppose) {
        this.oppose = oppose;
    }

    public String getSourceLink() {
        return sourceLink;
    }

    public void setSourceLink(String sourceLink) {
        this.sourceLink = sourceLink;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public void parseFields() {
        Map<CharSequence, CharSequence> fields = fieldGroup.getFields();

        CharSequence k = "";
        CharSequence v = "";

        k = "id";
        v = fields.get(k);
        if (v != null) setId(NumberUtils.toLong(v.toString(), 0));

        k = "name";
        v = fields.get(k);
        if (v != null) setName(v.toString());

        k = "title";
        v = fields.get(k);
        if (v != null) setTitle(v.toString());

        k = "author";
        v = fields.get(k);
        if (v != null) setAuthor(v.toString());

        k = "authors";
        v = fields.get(k);
        if (v != null) setAuthors(Arrays.asList(v.toString().split(",")));

        k = "directors";
        v = fields.get(k);
        if (v != null) setDirectors(Arrays.asList(v.toString().split(",")));

        k = "created";
        v = fields.get(k);
        if (v != null) setCreated(v.toString());

        k = "content";
        v = fields.get(k);
        if (v != null) setContent(v.toString());

        k = "reviewCount";
        v = fields.get(k);
        if (v != null) setReview(NumberUtils.toInt(v.toString(), 0));

        k = "forwardCount";
        v = fields.get(k);
        if (v != null) setForward(NumberUtils.toInt(v.toString(), 0));

        k = "voteCount";
        v = fields.get(k);
        if (v != null) setVote(NumberUtils.toInt(v.toString(), 0));

        k = "opposeCount";
        v = fields.get(k);
        if (v != null) setOppose(NumberUtils.toInt(v.toString(), 0));

        k = "sourceLink";
        v = fields.get(k);
        if (v != null) setSourceLink(v.toString());

        k = "sourceTitle";
        v = fields.get(k);
        if (v != null) setSourceTitle(v.toString());
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("id:\t").append(fieldGroup.getId());
        sb.append("parentId:\t").append(fieldGroup.getParentId());
        sb.append("group:\t").append(fieldGroup.getName());

        String fields = fieldGroup.getFields().entrySet().stream().map(e -> e.getKey() + ":\t" + e.getValue())
                .collect(Collectors.joining("\n"));
        sb.append("fields:\t").append(fields);
        return sb.toString();
    }

    public Map<String, Object> getFields() {
        Map<String, Object> result = new HashMap<>();
        result.put("id", String.valueOf(fieldGroup.getId()));
        result.put("parentId", String.valueOf(fieldGroup.getParentId()));
        result.put("group", fieldGroup.getName().toString());

        Map<String, String> fields = fieldGroup.getFields().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        result.put("fields", fields);
        return result;
    }

    @Override
    public String toString() {
        return format();
    }
}
