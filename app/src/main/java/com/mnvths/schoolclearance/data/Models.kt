package com.mnvths.schoolclearance.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Calendar

@Serializable
data class ClearanceItem(
    @SerialName("subjectName")
    val subjectName: String?,
    val schoolYear: String,
    val quarter: Int,
    val isCleared: Boolean
)

@Serializable
data class StudentProfile(
    val id: String,
    val name: String,
    val role: String,
    val gradeLevel: String,
    val section: String,
    val clearanceStatus: List<ClearanceItem>
)

@Serializable
data class OtherUser(
    val id: Int,
    val name: String,
    val role: String
)

sealed class LoggedInUser {
    data class StudentUser(val student: StudentProfile) : LoggedInUser()
    data class FacultyAdminUser(val user: OtherUser) : LoggedInUser()
}

@Serializable
data class Signatory(
    val id: Int,
    val name: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val username: String
)

@Serializable
data class Subject(
    val id: Int,
    @SerialName("subjectName")
    val name: String
)

@Serializable
data class AssignedSubject(
    @SerialName("signatoryId")
    val subjectId: Int,
    @SerialName("signatoryName")
    val subjectName: String
)

@Serializable
data class ClassSection(
    val sectionId: Int,
    val gradeLevel: String,
    val sectionName: String
)

@Serializable
data class AssignClassesRequest(
    val signatoryId: Int,
    val subjectId: Int,
    val sectionIds: List<Int>
)

@Serializable
data class AddSectionRequest(val gradeLevel: String, val sectionName: String)


@Serializable
data class AssignedSection(
    val sectionId: Int,
    val gradeLevel: String,
    val sectionName: String
)

@Serializable
data class StudentClearanceStatus(
    val userId: Int,
    val studentId: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val isCleared: Boolean
)

@Serializable
data class UpdateClearanceRequest(
    val userId: Int,
    val subjectId: Int,
    val sectionId: Int,
    val isCleared: Boolean
)

@Serializable
data class AppSettings(
    @SerialName("active_school_year")
    val activeSchoolYear: String = "${Calendar.getInstance().get(Calendar.YEAR)}-${Calendar.getInstance().get(Calendar.YEAR) + 1}",
    @SerialName("active_quarter_jhs")
    val activeQuarterJhs: String = "1",
    @SerialName("active_semester_shs")
    val activeSemesterShs: String = "1"
)

@Serializable
data class AddSubjectRequest(
    val subjectName: String
)

@Serializable
data class StudentListItem(
    val id: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val gradeLevel: String?,
    val sectionName: String?,
    val sectionId: Int?
)

@Serializable
data class ClearanceStatusItem(
    val signatoryName: String,
    val isCleared: Boolean
)

@Serializable
data class ActiveTerm(
    val schoolYear: String,
    val termName: String,
    val termNumber: String
)

@Serializable
data class AdminStudentProfile(
    val id: String,
    val name: String,
    val gradeLevel: String?,
    val section: String?,
    val clearanceStatus: List<ClearanceStatusItem>,
    val activeTerm: ActiveTerm,
    val sectionId: Int?,
    val firstName: String,
    val middleName: String?,
    val lastName: String
)


@Serializable
data class UpdateSectionRequest(val gradeLevel: String, val sectionName: String)

@Serializable
data class UpdateStudentRequest(
    val studentId: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val password: String?,
    val sectionId: Int?,
    val strandId: Int?,
    // ✅ ADDED: specializationId for updating student's enrollment
    val specializationId: Int?
)

@Serializable
data class Account(
    val id: Int,
    @SerialName("accountName")
    val name: String
)

@Serializable
data class StudentDetailsForEdit(
    val studentId: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val sectionId: Int?,
    val gradeLevel: String?,
    val strandId: Int?,
    // ✅ ADDED: Include current specialization to pre-select in dropdown
    val specializationId: Int?
)

@Serializable
data class GradeLevelItem(
    val id: Int,
    val name: String
)

@Serializable
data class ShsTrack(
    val id: Int,
    @SerialName("track_name")
    val trackName: String
)

@Serializable
data class ShsStrand(
    val id: Int,
    @SerialName("strand_name")
    val strandName: String,
    @SerialName("track_id")
    val trackId: Int
)

@Serializable
data class Specialization(
    val id: Int,
    @SerialName("subject_name")
    val name: String
)

// Add this new data class to your models.kt file
@Serializable
data class CreateStudentRequest(
    val studentId: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val password: String,
    val sectionId: Int?,
    val strandId: Int?,
    val specializationId: Int?
)