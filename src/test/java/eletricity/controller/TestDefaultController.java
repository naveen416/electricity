package eletricity.controller;

import eletricity.manager.TestMemberManager;
import eletricity.model.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.web.context.WebApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author kent
 */
@WebAppConfiguration
@ContextConfiguration(classes = eletricity.context.TestContext.class)
public class TestDefaultController extends AbstractTestNGSpringContextTests {

    private static Logger logger = LogManager.getLogger(TestDefaultController.class);
    @Autowired
    WebApplicationContext wac;
    private MockMvc mockMvc;
    @Autowired
    private TestMemberManager memberManager;

    @BeforeClass
    public void setup() {
        this.mockMvc = webAppContextSetup(this.wac).alwaysExpect(status().isOk()).build();
    }

    @Test
    void testDevice() throws Exception {
        mockMvc.perform(get("/").param("device", "mobile")).andExpect(view().name("index")).andExpect(model().attribute("device", is(equalTo("mobile"))));
    }

    @Test
    public void testListuser() throws Exception {
        mockMvc.perform(post("/admin/users")).andDo(print()).andExpect(jsonPath("$.total", is(equalTo(memberManager.countUsers()))));
    }

    @Test
    public void testMyinfo() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/user/myinfo").principal(new TestingAuthenticationToken("admin", null))).andReturn();
        Member member = (Member) mvcResult.getRequest().getAttribute("member");
        logger.debug("My account is \"{}\" and my name is {}", member.getAccount(), member.getName());
        assertThat("Test UserInfo error ", "admin", is(equalTo(member.getAccount())));
    }

    @Test
    public void testUserInfo() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/admin/user/{account}", "admin")).andReturn();
        Member member = (Member) mvcResult.getRequest().getAttribute("member");
        logger.debug("account \"{}\" name is {}", member.getAccount(), member.getName());
        assertThat("Test UserInfo error ", "admin", is(equalTo(member.getAccount())));
    }

}
