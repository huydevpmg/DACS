package com.dacs.vku.api

data class SignInResult(
    val user: UserData?,
    val errorMessage: String?
)