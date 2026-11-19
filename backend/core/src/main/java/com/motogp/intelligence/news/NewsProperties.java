package com.motogp.intelligence.news;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "motogp.news")
public class NewsProperties {
    private Scheduler scheduler = new Scheduler();
    private List<Source> sources = new ArrayList<>();

    public Scheduler getScheduler() { return scheduler; }
    public void setScheduler(Scheduler scheduler) { this.scheduler = scheduler; }
    public List<Source> getSources() { return sources; }
    public void setSources(List<Source> sources) { this.sources = sources == null ? new ArrayList<>() : sources; }

    public static class Scheduler {
        private boolean enabled;
        private long fixedDelayMs = 3600000L;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getFixedDelayMs() { return fixedDelayMs; }
        public void setFixedDelayMs(long fixedDelayMs) { this.fixedDelayMs = fixedDelayMs; }
    }

    public static class Source {
        private String code;
        private String name;
        private String sourceType = "RSS_ATOM";
        private String feedUrl;
        private boolean enabled = true;
        private int timeoutMs = 10000;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public String getFeedUrl() { return feedUrl; }
        public void setFeedUrl(String feedUrl) { this.feedUrl = feedUrl; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }

        public NewsSourceDefinition definition() {
            return new NewsSourceDefinition(code, name, sourceType, feedUrl, enabled, timeoutMs);
        }
    }
}
