package com.example.blog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiContractIntegrationTests extends ContainerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void statusUsesSecurityHeadersAndTraceId() throws Exception {
        mvc.perform(get("/api/v1/status").header("X-Trace-ID", "integration-trace-2026"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-ID", "integration-trace-2026"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src")))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void anonymousUserCannotAccessAdminApi() throws Exception {
        mvc.perform(get("/api/v1/admin/articles"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("exception"))));
    }

    @Test
    void demoRoleCanReadAdminApiButCannotMutateIt() throws Exception {
        var demoJwt = jwt()
                .jwt(token -> token.subject("7").claim("role", "DEMO"))
                .authorities(new SimpleGrantedAuthority("ROLE_DEMO"));
        mvc.perform(get("/api/v1/admin/articles")
                        .with(demoJwt))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/articles")
                        .with(demoJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Forbidden demo draft",
                                  "slug": "forbidden-demo-draft",
                                  "summary": "",
                                  "contentMarkdown": "# Not saved",
                                  "categoryId": null,
                                  "tagIds": [],
                                  "visibility": "PUBLIC",
                                  "pinned": false,
                                  "allowComment": true,
                                  "metaTitle": "",
                                  "metaDescription": "",
                                  "canonicalUrl": "",
                                  "version": 0
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void validationFailureUsesProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/public/articles")
                        .queryParam("keyword", "x".repeat(101))
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(
                        "https://personal-blog.local/problems/400"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void unknownCorsOriginIsRejected() throws Exception {
        mvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void sqlLikeSearchInputIsHandledAsData() throws Exception {
        mvc.perform(get("/api/v1/public/articles")
                        .queryParam("keyword", "' OR 1=1 --")
                        .queryParam("page", "1")
                        .queryParam("pageSize", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }
}
