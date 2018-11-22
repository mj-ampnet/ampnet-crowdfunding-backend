package com.ampnet.crowdfundingbackend.persistence.model

import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.Table

@Entity
@Table(name = "organization_follower")
@IdClass(OrganizationUserCompositeId::class)
data class OrganizationFollower(
    @Id
    var organizationId: Int,

    @Id
    var userId: Int,

    @Column(nullable = false)
    var createdAt: ZonedDateTime
)