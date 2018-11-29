package com.ampnet.crowdfundingbackend.config

import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class DatabaseCleanerService(val em: EntityManager) {

    @Transactional
    fun deleteAllUsers() {
        em.createNativeQuery("TRUNCATE app_user CASCADE").executeUpdate()
    }

    @Transactional
    fun deleteAllWalletsAndTransactions() {
        em.createNativeQuery("TRUNCATE wallet CASCADE").executeUpdate()
        em.createNativeQuery("TRUNCATE transaction CASCADE").executeUpdate()
    }

    @Transactional
    fun deleteAllOrganizations() {
        em.createNativeQuery("TRUNCATE organization CASCADE").executeUpdate()
    }

    @Transactional
    fun deleteAllOrganizationMemberships() {
        em.createNativeQuery("TRUNCATE organization_membership CASCADE").executeUpdate()
    }

    @Transactional
    fun deleteAllOrganizationFollowers() {
        em.createNativeQuery("TRUNCATE organization_follower CASCADE").executeUpdate()
    }

    @Transactional
    fun deleteAllOrganizationInvites() {
        em.createNativeQuery("TRUNCATE organization_invite CASCADE").executeUpdate()
    }
}
