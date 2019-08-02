package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Deposit
import com.ampnet.crowdfundingbackend.persistence.repository.DepositRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.service.DepositService
import com.ampnet.crowdfundingbackend.service.StorageService
import com.ampnet.crowdfundingbackend.service.TransactionInfoService
import com.ampnet.crowdfundingbackend.service.pojo.ApproveDepositRequest
import com.ampnet.crowdfundingbackend.service.pojo.MintServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class DepositServiceImpl(
    private val depositRepository: DepositRepository,
    private val walletRepository: UserWalletRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val storageService: StorageService
) : DepositService {

    private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')

    @Transactional
    override fun create(user: UUID): Deposit {
        if (walletRepository.findByUserUuid(user).isPresent.not()) {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User must have a wallet to create a Deposit")
        }
        val unapprovedDeposits = depositRepository.findByUserUuid(user).filter { it.approved.not() }
        if (unapprovedDeposits.isEmpty().not()) {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_DEPOSIT_EXISTS,
                    "Check your unapproved deposit: ${unapprovedDeposits.firstOrNull()?.id}")
        }

        val deposit = Deposit(0, user, generateDepositReference(), false,
            null, null, null, null, null, ZonedDateTime.now()
        )
        return depositRepository.save(deposit)
    }

    @Transactional
    override fun delete(id: Int) {
        depositRepository.deleteById(id)
    }

    @Transactional
    override fun approve(request: ApproveDepositRequest): Deposit {
        val deposit = depositRepository.findById(request.id).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_DEPOSIT_MISSING, "Missing deposit: ${request.id}")
        }
        // TODO: think about document reading restrictions
        val document = storageService.saveDocument(request.documentSaveRequest)

        deposit.approved = true
        deposit.approvedByUserUuid = request.user
        deposit.approvedAt = ZonedDateTime.now()
        deposit.amount = request.amount
        deposit.document = document
        return depositRepository.save(deposit)
    }

    @Transactional(readOnly = true)
    override fun getAllWithDocuments(approved: Boolean): List<Deposit> {
        return depositRepository.findAllWithDocument(approved)
    }

    @Transactional(readOnly = true)
    override fun findByReference(reference: String): Deposit? {
        return ServiceUtils.wrapOptional(depositRepository.findByReference(reference))
    }

    @Transactional
    override fun generateMintTransaction(request: MintServiceRequest): TransactionDataAndInfo {
        val deposit = getDepositForId(request.depositId)
        throwExceptionIfDepositHasTxHash(deposit)
        throwExceptionIfDepositIsNotApproved(deposit)
        val senderWallet = "not-needed"
        val data = blockchainService.generateMintTransaction(senderWallet, request.toWallet, request.amount)
        val info = transactionInfoService.createMintTransaction(request)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun confirmMintTransaction(signedTransaction: String, depositId: Int): Deposit {
        val deposit = getDepositForId(depositId)
        throwExceptionIfDepositHasTxHash(deposit)
        throwExceptionIfDepositIsNotApproved(deposit)
        val txHash = blockchainService.postTransaction(signedTransaction, PostTransactionType.ISSUER_MINT)
        deposit.txHash = txHash
        return depositRepository.save(deposit)
    }

    private fun throwExceptionIfDepositIsNotApproved(deposit: Deposit) {
        if (deposit.approved.not()) {
            throw InvalidRequestException(ErrorCode.WALLET_DEPOSIT_NOT_APPROVED,
                    "Deposit: ${deposit.id} is not approved")
        }
    }

    private fun throwExceptionIfDepositHasTxHash(deposit: Deposit) {
        if (deposit.txHash != null) {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_DEPOSIT_MINTED, "Mint txHash: ${deposit.txHash}")
        }
    }

    private fun getDepositForId(depositId: Int): Deposit {
        return depositRepository.findById(depositId).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_DEPOSIT_MISSING,
                    "For mint transaction missing deposit: $depositId")
        }
    }

    private fun generateDepositReference(): String = (1..8)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
