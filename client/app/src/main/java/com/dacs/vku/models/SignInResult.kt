package com.dacs.vku.models

data class SignInResult(
    val user: UserData?,
    val errorMessage: String?
)