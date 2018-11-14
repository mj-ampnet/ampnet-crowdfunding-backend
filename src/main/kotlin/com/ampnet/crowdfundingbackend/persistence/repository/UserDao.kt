package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserDao : JpaRepository<User, Int> {

    fun findByEmail(email: String): Optional<User>

    @Query("SELECT u FROM User u INNER JOIN u.organizations mem WHERE mem.organizationId = ?1")
    fun findAllUserForOrganization(organizationId: Int): List<User>
}
