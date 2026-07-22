package com.example.blog.github.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.example.blog.github.config.GitHubContributionProperties;
import com.example.blog.github.dto.GitHubContributionDay;
import com.example.blog.github.dto.PublicGitHubContributions;
import com.example.blog.github.mapper.GitHubContributionMapper;
import com.example.blog.github.model.GitHubContributionSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GitHubContributionService {

    private static final Logger log = LoggerFactory.getLogger(GitHubContributionService.class);
    private static final String CONTRIBUTIONS_QUERY = """
            query($login: String!, $from: DateTime!, $to: DateTime!) {
              user(login: $login) {
                contributionsCollection(from: $from, to: $to) {
                  contributionCalendar {
                    totalContributions
                    weeks {
                      contributionDays {
                        date
                        contributionCount
                      }
                    }
                  }
                }
              }
            }
            """;

    private final GitHubContributionMapper mapper;
    private final ObjectMapper objectMapper;
    private final GitHubContributionProperties properties;
    private final RestClient restClient;
    private final Clock clock;

    @Autowired
    public GitHubContributionService(
            GitHubContributionMapper mapper,
            ObjectMapper objectMapper,
            GitHubContributionProperties properties
    ) {
        this(mapper, objectMapper, properties,
                RestClient.builder().baseUrl("https://api.github.com/graphql").build(), Clock.systemUTC());
    }

    GitHubContributionService(
            GitHubContributionMapper mapper,
            ObjectMapper objectMapper,
            GitHubContributionProperties properties,
            RestClient restClient,
            Clock clock
    ) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.restClient = restClient;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncAfterStartup() {
        syncSafely("startup");
    }

    @Scheduled(
            cron = "${blog.github-contributions.cron:0 10 8 * * *}",
            zone = "${blog.github-contributions.zone:Asia/Shanghai}"
    )
    public void syncDaily() {
        syncSafely("scheduled");
    }

    public PublicGitHubContributions publicSnapshot() {
        return mapper.findSnapshot()
                .map(this::toPublicSnapshot)
                .orElseGet(() -> new PublicGitHubContributions(
                        configuredUsername(), 0, List.of(), null
                ));
    }

    @Transactional
    public void sync() {
        if (!properties.readyToSync()) return;

        RemoteContributionSnapshot remote = fetchRemoteSnapshot();
        try {
            mapper.upsertSnapshot(
                    configuredUsername(),
                    remote.totalContributions(),
                    objectMapper.writeValueAsString(remote.days())
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化 GitHub 贡献数据", exception);
        }
    }

    private void syncSafely(String trigger) {
        if (!properties.readyToSync()) {
            log.debug("GitHub contribution synchronization is disabled or incomplete");
            return;
        }
        try {
            sync();
            log.info("GitHub contribution snapshot refreshed ({})", trigger);
        } catch (RuntimeException exception) {
            log.warn("GitHub contribution synchronization failed ({}); serving the last snapshot", trigger, exception);
        }
    }

    private RemoteContributionSnapshot fetchRemoteSnapshot() {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        Instant from = today.minusDays(364).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<String, Object> request = Map.of(
                "query", CONTRIBUTIONS_QUERY,
                "variables", Map.of(
                        "login", configuredUsername(),
                        "from", from.toString(),
                        "to", to.toString()
                )
        );
        String responseBody = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token().trim())
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .body(request)
                .retrieve()
                .body(String.class);

        JsonNode response;
        try {
            response = responseBody == null ? null : objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析 GitHub GraphQL 响应", exception);
        }

        if (response == null || response.hasNonNull("errors")) {
            throw new IllegalStateException("GitHub GraphQL returned an error response");
        }
        JsonNode calendar = response.path("data")
                .path("user")
                .path("contributionsCollection")
                .path("contributionCalendar");
        if (calendar.isMissingNode() || calendar.isNull()) {
            throw new IllegalStateException("GitHub GraphQL response did not contain a contribution calendar");
        }
        return new RemoteContributionSnapshot(
                calendar.path("totalContributions").asInt(),
                readDays(calendar.path("weeks"))
        );
    }

    private List<GitHubContributionDay> readDays(JsonNode weeks) {
        TreeMap<LocalDate, Integer> days = new TreeMap<>();
        for (JsonNode week : weeks) {
            for (JsonNode day : week.path("contributionDays")) {
                try {
                    LocalDate date = LocalDate.parse(day.path("date").asText());
                    days.put(date, Math.max(0, day.path("contributionCount").asInt()));
                } catch (RuntimeException ignored) {
                    // Ignore malformed upstream entries and preserve the last good snapshot.
                }
            }
        }
        return days.entrySet().stream()
                .map(entry -> new GitHubContributionDay(entry.getKey(), entry.getValue()))
                .toList();
    }

    private PublicGitHubContributions toPublicSnapshot(GitHubContributionSnapshot snapshot) {
        return new PublicGitHubContributions(
                snapshot.username(),
                snapshot.totalContributions(),
                readStoredDays(snapshot.contributionsJson()),
                snapshot.syncedAt().toInstant(ZoneOffset.UTC)
        );
    }

    private List<GitHubContributionDay> readStoredDays(String contributionsJson) {
        try {
            return objectMapper.readValue(contributionsJson, new TypeReference<List<GitHubContributionDay>>() { });
        } catch (JsonProcessingException exception) {
            log.warn("Ignoring an invalid stored GitHub contribution snapshot", exception);
            return List.of();
        }
    }

    private String configuredUsername() {
        return properties.username() == null ? "" : properties.username().trim();
    }

    private record RemoteContributionSnapshot(int totalContributions, List<GitHubContributionDay> days) {
    }
}
