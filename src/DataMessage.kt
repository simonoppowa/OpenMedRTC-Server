package software.openmedrtc

import software.openmedrtc.helper.JsonRequired

data class DataMessage(@JsonRequired val messageType: String, @JsonRequired val json: String)
