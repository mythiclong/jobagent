package com.zhihang.jobagent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HistoryAndAdminRecordPageTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void historyCenterShouldRequireLoginForGuest() throws Exception {
        mockMvc.perform(get("/history-center"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void historyCenterShouldOpenForLoggedInUser() throws Exception {
        mockMvc.perform(get("/history-center").with(user("demo").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("History Center")));
    }

    @Test
    void adminUserRecordsShouldRequireAdminRole() throws Exception {
        mockMvc.perform(get("/admin/user-records"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/admin/user-records").with(user("demo").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/forbidden"));
    }

    @Test
    void adminUserRecordsShouldOpenForAdmin() throws Exception {
        mockMvc.perform(get("/admin/user-records").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Admin Records")));
    }

    @Test
    void trailingSlashPathsShouldAlsoWork() throws Exception {
        mockMvc.perform(get("/history-center/").with(user("demo").roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/user-records/").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
