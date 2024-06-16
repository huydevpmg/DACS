package com.dacs.vku.models

import java.io.Serializable

data class UserData(
    val username: String?,
    val email: String?,
    val userId: String?,
    val profilePictureUrl: String?
) : Serializable
