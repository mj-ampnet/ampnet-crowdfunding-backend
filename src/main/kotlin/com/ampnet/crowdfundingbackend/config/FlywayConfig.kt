package com.ampnet.crowdfundingbackend.config

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FlywayConfig {

    @Value("\${spring.datasource.url}")
    private lateinit var databaseUrl: String

    @Value("\${spring.datasource.username}")
    private lateinit var username: String

    @Value("\${spring.datasource.password}")
    private lateinit var password: String

    private val role = "cf_role"

    @Bean
    fun flyway(): Flyway {
        val flywayConfig = Flyway.configure()
            .dataSource(databaseUrl, username, password)
            .initSql("SET ROLE $role")
        val flyway = flywayConfig.load()
        flyway.migrate()
        return flyway
    }
}