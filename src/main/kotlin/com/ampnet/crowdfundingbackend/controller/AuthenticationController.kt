package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.TokenProvider
import com.ampnet.crowdfundingbackend.controller.pojo.request.TokenRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.TokenRequestSocialInfo
import com.ampnet.crowdfundingbackend.controller.pojo.request.TokenRequestUserInfo
import com.ampnet.crowdfundingbackend.controller.pojo.response.AuthTokenResponse
import com.ampnet.crowdfundingbackend.exception.InvalidLoginMethodException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthenticationController(
    val authenticationManager: AuthenticationManager,
    val jwtTokenUtil: TokenProvider,
    val userService: UserService,
    val socialService: SocialService,
    val objectMapper: ObjectMapper
) {

    companion object : KLogging()

    @PostMapping("token")
    fun generateToken(@RequestBody tokenRequest: TokenRequest): ResponseEntity<AuthTokenResponse> {
        logger.debug { "Received request for token: $tokenRequest" }
        val usernamePasswordAuthenticationToken = when (tokenRequest.loginMethod) {
            AuthMethod.EMAIL -> {
                val userInfo: TokenRequestUserInfo = objectMapper.convertValue(tokenRequest.credentials)
                validateLoginParamsOrThrowException(userInfo.email, AuthMethod.EMAIL)
                UsernamePasswordAuthenticationToken(userInfo.email, userInfo.password)
            }
            AuthMethod.FACEBOOK -> {
                val userInfo: TokenRequestSocialInfo = objectMapper.convertValue(tokenRequest.credentials)
                val facebookProfile = socialService.getFacebookUserInfo(userInfo.token)
                validateLoginParamsOrThrowException(facebookProfile.email, AuthMethod.FACEBOOK)
                UsernamePasswordAuthenticationToken(facebookProfile.email, null)
            }
            AuthMethod.GOOGLE -> {
                val userInfo: TokenRequestSocialInfo = objectMapper.convertValue(tokenRequest.credentials)
                val googleProfile = socialService.getGoogleUserInfo(userInfo.token)
                validateLoginParamsOrThrowException(googleProfile.email, AuthMethod.GOOGLE)
                UsernamePasswordAuthenticationToken(googleProfile.email, null)
            }
        }
        val authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken)
        SecurityContextHolder.getContext().authentication = authentication
        val token = jwtTokenUtil.generateToken(authentication)

        logger.debug { "User successfully authenticated." }
        return ResponseEntity.ok(AuthTokenResponse(token))
    }

    private fun validateLoginParamsOrThrowException(email: String, loginMethod: AuthMethod) {
        val storedUser = userService.find(email)
                ?: throw ResourceNotFoundException("User with email: $email does not exists")
        val userAuthMethod = storedUser.authMethod
        if (userAuthMethod != loginMethod) {
            throw InvalidLoginMethodException("Invalid method. Try to login using ${userAuthMethod.name}")
        }
    }
}
