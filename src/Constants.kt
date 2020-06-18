package software.openmedrtc

object Constants {
    const val AUTHENTICATION_KEY_BASIC = "basicAuth"
    const val AUTHENTICATION_REALM_BASIC = "basicRealm"

    const val PATH_WEBSITE = "/"
    const val PATH_WEBSOCKET = "/connect"
    const val PATH_REST = "/rest"
    const val PATH_USER_KEY = "userKey"

    const val MESSAGE_TYPE_PATIENTS_LIST = "PATIENTS_LIST"
    const val MESSAGE_TYPE_SDP_OFFER = "SDP_OFFER"
    const val MESSAGE_TYPE_SDP_ANSWER = "SDP_ANSWER"
    const val MESSAGE_TYPE_ICE_CANDIDATE = "ICE_CANDIDATE"
}
