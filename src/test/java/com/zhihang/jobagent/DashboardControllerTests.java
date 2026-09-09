package com.zhihang.jobagent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void guestShouldOnlySeeGuestNavigationAndPublicPages() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/login\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/register\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/career-profile\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/jobs\""))));

        mockMvc.perform(get("/about"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void guestShouldBeRedirectedWhenAccessingProtectedPages() throws Exception {
        mockMvc.perform(get("/profiles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/match"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/resume-review"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/interview"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/jobs"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void userRoleShouldAccessUserPagesButNotAdminPages() throws Exception {
        mockMvc.perform(get("/").with(user("demo").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/career-profile\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/jobs\""))));

        mockMvc.perform(get("/profiles").with(user("demo").roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/match").with(user("demo").roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/my-records").with(user("demo").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/history-center"));

        mockMvc.perform(get("/jobs").with(user("demo").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/forbidden"));
    }

    @Test
    void adminRoleShouldAccessAdminPagesAndShowAdminNavigation() throws Exception {
        mockMvc.perform(get("/").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/jobs\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/career-profile\""))));

        mockMvc.perform(get("/jobs").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void loginAndLogoutShouldSwitchSessionState() throws Exception {
        MvcResult loginResult = mockMvc.perform(formLogin().user("demo").password("123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/career-profile"))
                .andReturn();

        mockMvc.perform(post("/logout")
                        .session((MockHttpSession) loginResult.getRequest().getSession(false))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }
}
