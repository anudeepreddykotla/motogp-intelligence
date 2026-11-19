package com.motogp.intelligence.news;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class RssAtomNewsSourceAdapter implements NewsSourceAdapter {
    private static final String TYPE = "RSS_ATOM";

    @Override
    public String sourceType() {
        return TYPE;
    }

    @Override
    public List<NewsArticleRecord> fetch(NewsSourceDefinition source) {
        if (source.feedUrl() == null || source.feedUrl().isBlank()) {
            throw new NewsSourceAdapterException("News source has no feed URL: " + source.code());
        }
        try (InputStream input = open(source.feedUrl(), source.timeoutMs())) {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            setXmlSecurity(factory);
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            List<NewsArticleRecord> articles = new ArrayList<>();
            Entry entry = null;
            String field = null;
            StringBuilder text = new StringBuilder();
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String name = reader.getLocalName().toLowerCase();
                    if (name.equals("item") || name.equals("entry")) {
                        entry = new Entry();
                    } else if (entry != null && name.equals("link")) {
                        String href = reader.getAttributeValue(null, "href");
                        if (href != null && !href.isBlank()) entry.originalUrl = href.trim();
                        field = "link";
                        text.setLength(0);
                    } else if (entry != null && (name.equals("title") || name.equals("description")
                            || name.equals("summary") || name.equals("content") || name.equals("pubdate")
                            || name.equals("published") || name.equals("updated"))) {
                        String media = reader.getAttributeValue(null, "url");
                        if (media == null) media = reader.getAttributeValue(null, "href");
                        String mediaType = reader.getAttributeValue(null, "type");
                        if (name.equals("content") && media != null
                                && (mediaType == null || mediaType.toLowerCase().startsWith("image/"))) {
                            entry.imageUrl = media.trim();
                        }
                        field = name;
                        text.setLength(0);
                    } else if (entry != null && (name.equals("enclosure") || name.equals("content"))) {
                        String image = reader.getAttributeValue(null, "url");
                        if (image == null) image = reader.getAttributeValue(null, "href");
                        String mediaType = reader.getAttributeValue(null, "type");
                        if (image != null && (mediaType == null || mediaType.toLowerCase().startsWith("image/"))) {
                            entry.imageUrl = image.trim();
                        }
                    }
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    if (field != null) text.append(reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String name = reader.getLocalName().toLowerCase();
                    if (entry != null && field != null && field.equals(name)) {
                        entry.assign(field, text.toString().trim());
                        field = null;
                        text.setLength(0);
                    }
                    if (entry != null && (name.equals("item") || name.equals("entry"))) {
                        if (entry.title != null && !entry.title.isBlank()) articles.add(entry.toArticle());
                        entry = null;
                        field = null;
                        text.setLength(0);
                    }
                }
            }
            reader.close();
            if (articles.isEmpty()) throw new NewsSourceAdapterException("Feed contained no usable news entries: " + source.code());
            return articles;
        } catch (NewsSourceAdapterException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NewsSourceAdapterException("Unable to read news feed " + source.code(), exception);
        }
    }

    private InputStream open(String location, int timeoutMs) throws Exception {
        if (location.startsWith("classpath:")) {
            String path = location.substring("classpath:".length());
            return new ClassPathResource(path.startsWith("/") ? path.substring(1) : path).getInputStream();
        }
        URL url = URI.create(location).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml");
        connection.setRequestProperty("User-Agent", "MotoGP-Intelligence-NewsAdapter/1.0");
        if (connection.getResponseCode() >= 400) {
            throw new NewsSourceAdapterException("News feed returned HTTP " + connection.getResponseCode());
        }
        return connection.getInputStream();
    }

    private void setXmlSecurity(XMLInputFactory factory) throws IllegalArgumentException {
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Instant.parse(value); } catch (DateTimeParseException ignored) { }
        try { return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); }
        catch (DateTimeParseException ignored) { return null; }
    }

    private static String canonicalize(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value.trim()).normalize();
            return new URI(uri.getScheme() == null ? null : uri.getScheme().toLowerCase(),
                    uri.getUserInfo(), uri.getHost() == null ? null : uri.getHost().toLowerCase(),
                    uri.getPort(), uri.getPath(), uri.getQuery(), null).toString();
        } catch (Exception ignored) {
            return value.trim();
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append("%02x".formatted(item));
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static class Entry {
        private String title;
        private String originalUrl;
        private String description;
        private String published;
        private String imageUrl;

        private void assign(String field, String value) {
            switch (field) {
                case "title" -> title = value;
                case "link" -> { if (originalUrl == null || originalUrl.isBlank()) originalUrl = value; }
                case "description", "summary", "content" -> { if (description == null || description.isBlank()) description = value; }
                case "pubdate", "published", "updated" -> { if (published == null || published.isBlank()) published = value; }
                default -> { }
            }
        }

        private NewsArticleRecord toArticle() {
            String canonical = canonicalize(originalUrl);
            Instant publication = parseInstant(published);
            String fingerprint = sha256((title == null ? "" : title) + "\n"
                    + (description == null ? "" : description) + "\n" + (published == null ? "" : published));
            String deduplicationKey = canonical == null ? "fingerprint:" + fingerprint : "url:" + canonical;
            return new NewsArticleRecord(title.trim(), originalUrl, canonical == null ? "urn:news:" + fingerprint : canonical,
                    publication, description, imageUrl, sha256(title + "\n" + (description == null ? "" : description)),
                    deduplicationKey);
        }
    }
}
