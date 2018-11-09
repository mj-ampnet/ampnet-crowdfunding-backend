package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.config.DatabaseCleanerService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.restdocs.JUnitRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@RunWith(SpringRunner::class)
@SpringBootTest
abstract class ControllerTestBase : TestBase() {

    protected val defaultEmail = "user@email.com"

    @Autowired
    private lateinit var wac: WebApplicationContext

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService

    protected lateinit var mockMvc: MockMvc

    @get:Rule
    var restDocumentation = JUnitRestDocumentation()

    @Before
    fun init() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
                .alwaysDo<DefaultMockMvcBuilder>(MockMvcRestDocumentation.document(
                        "{ClassName}/{methodName}",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
                ))
                .build()
    }
}
