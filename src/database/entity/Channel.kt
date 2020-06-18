package software.openmedrtc.database.entity

import software.openmedrtc.dto.PatientDTO

data class Channel(
    val hostSession: MedicalConnectionSession,
    val patientSessions: MutableList<PatientConnectionSession>
) {
    fun hasPatientConnected(patient: Patient): Boolean =
        patientSessions.map { it.patient.email }.contains(patient.email)

    fun mapPatientsDTOOnline(): List<PatientDTO> = patientSessions.map { PatientDTO(it.patient) }

}
