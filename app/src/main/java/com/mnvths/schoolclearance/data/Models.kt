package com.mnvths.schoolclearance.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Calendar

@Serializable
data class ClearanceItem(
    @SerialName("subjectName") // This name from the server is now correct
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

// ✅ RENAMED: FacultyMember is now Signatory (represents a person)
@Serializable
data class Signatory(
    val id: Int,
    val name: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val username: String
)

// ✅ RENAMED: Signatory is now Subject (represents a clearance item)
@Serializable
data class Subject(
    val id: Int,
    @SerialName("subjectName") // Matches the API response key
    val name: String
)

// ✅ RENAMED: Represents a subject assigned to a signatory
@Serializable
data class AssignedSubject(
    // NOTE: Your API might still call these signatoryId/Name. @SerialName handles it.
    // Renaming the Kotlin properties makes the app code easier to understand.
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

// ✅ UPDATED: Now uses the correct property names
@Serializable
data class AssignClassesRequest(
    val signatoryId: Int,
    val subjectId: Int,
    val sectionIds: List<Int>
)

@Serializable
data class AddSectionRequest(val gradeLevel: String, val sectionName: String)


@Serializable
data class AddStudentRequest(
    val studentId: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val sectionId: Int
)

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

// ✅ UPDATED: Now uses the correct property name
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

// ✅ RENAMED: For adding a new subject
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
    // This was already correct, just confirming
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
    val gradeLevel: String,
    val section: String,
    val clearanceStatus: List<ClearanceStatusItem>,
    val activeTerm: ActiveTerm,
    val sectionId: Int?,
    val firstName: String,
    val middleName: String?,
    val lastName: String
)


@Serializable
data class UpdateSectionRequest(val gradeLevel: String, val sectionName: String)

// ✅ MODIFIED: sectionId is now nullable to allow unassigning a student from a section
@Serializable
data class UpdateStudentRequest(
    val studentId: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val password: String?,
    val sectionId: Int?
)

@Serializable
data class Account(
    val id: Int,
    @SerialName("accountName")
    val name: String
)