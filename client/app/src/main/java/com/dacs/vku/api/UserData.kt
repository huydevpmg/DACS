package com.dacs.vku.api

import java.io.Serializable

data class UserData(
    val username: String?,
    val email: String?,
    val userId: String?,
    val profilePictureUrl: String?
) : Serializable
