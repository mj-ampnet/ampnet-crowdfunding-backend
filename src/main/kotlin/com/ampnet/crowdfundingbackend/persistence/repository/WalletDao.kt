package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface WalletDao : JpaRepository<Wallet, Int> {
    fun findByOwnerId(ownerId: Int): Optional<Wallet>

    @Query("SELECT w FROM Wallet w LEFT JOIN FETCH w.transactions trans WHERE w.ownerId = ?1")
    fun findByOwnerIdWithTransactions(ownerId: Int): Optional<Wallet>
}
