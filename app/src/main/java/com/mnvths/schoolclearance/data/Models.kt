package com.mnvths.schoolclearance.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Calendar

@Serializable
data class ClearanceItem(
    // ✅ FIX 1: Use @SerialName to match the key from server.js ("subjectName")
    @SerialName("subjectName")
    val signatoryName: String?,
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

// NEW data class for a faculty member
@Serializable
data class FacultyMember(
    val id: Int,
    val name: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val username: String
)

// NEW data class for a signatory
@Serializable
data class Signatory(
    val id: Int,
    val signatoryName: String
)

// NEW data class for an assigned signatory
@Serializable
data class AssignedSignatory(
    val signatoryId: Int,
    val signatoryName: String
)

// NEW data class for a class section
@Serializable
data class ClassSection(
    val sectionId: Int,
    val gradeLevel: String,
    val sectionName: String
)

@Serializable
data class AssignClassesRequest(
    val facultyId: Int,
    val signatoryId: Int,
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

// Data model for the update request
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
    // ✅ FIX: Replaced Year.now().value with Calendar to support older Android versions
    val activeSchoolYear: String = "${Calendar.getInstance().get(Calendar.YEAR)}-${Calendar.getInstance().get(Calendar.YEAR) + 1}",
    @SerialName("active_quarter_jhs")
    val activeQuarterJhs: String = "1",
    @SerialName("active_semester_shs")
    val activeSemesterShs: String = "1"
)


@Serializable
data class AddSignatoryRequest(
    val signatoryName: String
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
    val gradeLevel: String,
    val section: String,
    val clearanceStatus: List<ClearanceStatusItem>,
    val activeTerm: ActiveTerm,
    // Add these fields to use for populating the edit screen
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
    val sectionId: Int?
)