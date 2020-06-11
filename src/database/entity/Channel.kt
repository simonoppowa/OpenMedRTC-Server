package software.openmedrtc.database.entity

data class Channel(
    val hostSession: MedicalConnectionSession,
    val patientSessions: MutableList<PatientConnectionSession>
) {
    fun hasPatientConnected(patient: Patient): Boolean =
        patientSessions.map { it.patient.email }.contains(patient.email)
}
