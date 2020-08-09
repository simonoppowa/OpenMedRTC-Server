package software.openmedrtc.exception

open class ConnectionException(message: String) : Exception(message)
class SocketConnectionException(message: String) : ConnectionException(message)
class HttpConnectionException(message: String) : ConnectionException(message)
