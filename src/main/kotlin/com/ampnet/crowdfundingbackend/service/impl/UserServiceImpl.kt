package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.controller.pojo.request.UserUpdateRequest
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.MailToken
import com.ampnet.crowdfundingbackend.persistence.model.Role
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.CountryDao
import com.ampnet.crowdfundingbackend.persistence.repository.MailTokenDao
import com.ampnet.crowdfundingbackend.persistence.repository.RoleDao
import com.ampnet.crowdfundingbackend.persistence.repository.UserDao
import com.ampnet.crowdfundingbackend.service.MailService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import mu.KLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class UserServiceImpl(
    private val userDao: UserDao,
    private val roleDao: RoleDao,
    private val countryDao: CountryDao,
    private val mailTokenDao: MailTokenDao,
    private val mailService: MailService,
    private val passwordEncoder: PasswordEncoder
) : UserService {

    companion object : KLogging()

    private val userRole: Role by lazy {
        roleDao.getOne(UserRoleType.USER.id)
    }

    private val adminRole: Role by lazy {
        roleDao.getOne(UserRoleType.ADMIN.id)
    }

    @Transactional
    override fun create(request: CreateUserServiceRequest): User {
        if (userDao.findByEmail(request.email).isPresent) {
            logger.info { "Trying to create user with email that already exists: ${request.email}" }
            throw ResourceAlreadyExistsException("User with email: ${request.email} already exists!")
        }

        val userRequest = createUserFromRequest(request)
        val user = userDao.save(userRequest)

        if (user.authMethod == AuthMethod.EMAIL) {
            val mailToken = createMailToken(user)
            mailService.sendConfirmationMail(user.email, mailToken.token.toString())
        }

        return user
    }

    @Transactional
    override fun update(request: UserUpdateRequest): User {
        val savedUser = userDao.findByEmail(request.email).orElseThrow {
            logger.info { "Trying to update user with email ${request.email} which does not exists in db." }
            throw ResourceNotFoundException("User with email: ${request.email} does not exists")
        }
        val user = updateUserFromRequest(savedUser, request)
        return userDao.save(user)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<User> {
        return userDao.findAll()
    }

    @Transactional(readOnly = true)
    override fun find(username: String): User? {
        return ServiceUtils.wrapOptional(userDao.findByEmail(username))
    }

    @Transactional(readOnly = true)
    override fun find(id: Int): User? {
        return ServiceUtils.wrapOptional(userDao.findById(id))
    }

    @Transactional
    override fun delete(id: Int) {
        userDao.deleteById(id)
    }

    @Transactional
    override fun confirmEmail(token: UUID): User? {
        val optionalMailToken = mailTokenDao.findByToken(token)
        if (!optionalMailToken.isPresent) {
            return null
        }
        val mailToken = optionalMailToken.get()
        if (mailToken.isExpired()) {
            logger.info { "User is trying to confirm mail with expired token: $token" }
            throw InvalidRequestException("The token: $token has expired")
        }
        val user = mailToken.user
        user.enabled = true

        mailTokenDao.delete(mailToken)
        return userDao.save(user)
    }

    @Transactional
    override fun resendConfirmationMail(user: User) {
        if (user.authMethod != AuthMethod.EMAIL) {
            return
        }

        mailTokenDao.findByUserId(user.id).ifPresent {
            mailTokenDao.delete(it)
        }
        val mailToken = createMailToken(user)
        mailService.sendConfirmationMail(user.email, mailToken.token.toString())
    }

    private fun createUserFromRequest(request: CreateUserServiceRequest): User {
        val user = User::class.java.newInstance()
        user.email = request.email
        user.password = passwordEncoder.encode(request.password.orEmpty())
        user.firstName = request.firstName
        user.lastName = request.lastName
        user.phoneNumber = request.phoneNumber
        user.role = userRole
        user.createdAt = ZonedDateTime.now()
        user.authMethod = request.authMethod

        if (user.authMethod == AuthMethod.EMAIL) {
            // user must confirm email
            user.enabled = false
        } else {
            // social user is confirmed from social service
            user.enabled = true
        }

        request.countryId?.let { id ->
            user.country = countryDao.findById(id).orElse(null)
        }
        return user
    }

    private fun updateUserFromRequest(user: User, request: UserUpdateRequest): User {
        user.firstName = request.firstName
        user.lastName = request.lastName
        user.phoneNumber = request.phoneNumber
        user.country = countryDao.findById(request.countryId).orElseThrow {
            throw ResourceNotFoundException("Country with id: ${request.countryId} does not exists")
        }
        return user
    }

    private fun createMailToken(user: User): MailToken {
        val mailToken = MailToken::class.java.newInstance()
        mailToken.user = user
        mailToken.token = generateToken()
        mailToken.createdAt = ZonedDateTime.now()
        return mailTokenDao.save(mailToken)
    }

    private fun generateToken(): UUID = UUID.randomUUID()
}
