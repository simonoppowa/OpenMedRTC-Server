package software.openmedrtc.database.entity

data class Channel(val hostSession: MedicalConnectionSession, val patientSession: MutableList<PatientConnectionSession>)
