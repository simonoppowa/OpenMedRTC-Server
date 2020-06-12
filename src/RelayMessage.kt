package software.openmedrtc

import software.openmedrtc.helper.JsonRequired

data class RelayMessage(
    @JsonRequired val fromUser: String,
    @JsonRequired val toUser: String,
    val content: String
)
