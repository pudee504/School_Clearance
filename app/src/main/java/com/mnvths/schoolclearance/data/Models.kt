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
    val isCleared: Boolean,
    @SerialName("signatoryName")
    val signatoryName: String?,
    val requirementType: String
)

@Serializable
data class StudentProfile(
    val userId: Int,
    val id: String,
    val name: String,
    val role: String,
    val gradeLevel: String?, // Before: String
    val section: String?,
    val clearanceStatus: List<ClearanceItem>
)

@Serializable
data class OtherUser(
    val id: Int,
    val name: String,
    val role: String,
    val username: String?
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

// ✅ CORRECTED THIS DATA CLASS
@Serializable
data class AssignedSubject(
    @SerialName("subjectId") // Changed from "signatoryId"
    val subjectId: Int,
    @SerialName("subjectName") // Changed from "signatoryName"
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
    val isCleared: Boolean,
    val isClearable: Boolean // <-- ADD THIS FIELD
)

// ✅ REPLACED: This old request model is replaced by the more detailed one below.
@Serializable
data class UpdateClearanceStatusRequest(
    val userId: Int,
    val requirementId: Int,
    val schoolYear: String,
    val term: String,
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

// ✅ MODIFIED: This model is now more detailed to support the new UI and logic.
@Serializable
data class ClearanceStatusItem(
    val clearanceId: Int?,
    val requirementId: Int,
    val requirementName: String?, // Renamed from signatoryName
    val signatoryName: String?,   // ✅ ADDED THIS NEW FIELD
    val isCleared: Boolean,
    val requirementType: String
)

@Serializable
data class ActiveTerm(
    val schoolYear: String,
    val termName: String,
    val termNumber: String
)

// ✅ MODIFIED: Added the student's internal user ID.
@Serializable
data class AdminStudentProfile(
    val id: String,
    val userId: Int,
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
    val gradeLevelId: Int,
    val sectionId: Int?,
    val strandId: Int?,
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
    val gradeLevelId: Int?, // ✅ ADDED: To know the student's current grade level ID
    val strandId: Int?,
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

@Serializable
data class CreateStudentRequest(
    val studentId: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val password: String,
    val gradeLevelId: Int, // ✅ ADDED: Grade level is now required
    val sectionId: Int?,
    val strandId: Int?,
    val specializationId: Int?
)

@Serializable
data class CurriculumSubject(
    val subjectId: Int,
    val subjectName: String,
    val gradeLevel: String?,
    val gradeLevelId: Int?,
    val strandName: String?,
    val semester: Int?,
    val display_order: Int
)


@Serializable
data class CurriculumResponse(
    val subjects: List<CurriculumSubject>,
    val activeSemester: String
)

data class SubjectGroup(
    val title: String,
    val subjects: List<CurriculumSubject>
)

// ✅ NEW DATA CLASS
@Serializable
data class AssignedAccount(
    @SerialName("accountId")
    val accountId: Int,
    @SerialName("accountName")
    val accountName: String
)

@Serializable
data class AssignSectionsToSubjectRequest(
    val signatoryId: Int,
    val subjectId: Int,
    val sectionIds: List<Int>
)

@Serializable
data class AssignSectionsToAccountRequest(
    val signatoryId: Int,
    val accountId: Int,
    val sectionIds: List<Int>
)
// Add this new data class inside the ViewModel file, or in your data package
@Serializable
data class ClearMultipleRequest(
    val requirementId: Int,
    val schoolYear: String,
    val term: String,
    val studentUserIds: List<Int>
)
// Add to your data package
// The request body should probably include the semester
@Serializable
data class AddSubjectWithGradeRequest(
    val subjectName: String,
    val gradeLevelId: Int,
    val semester: Int,
    val subjectCode: String
)

// ✅ ADDED: Represents a subject in the management screen
@Serializable
data class CurriculumManagementSubject(
    val requirementId: Int,
    val subjectId: Int,
    val subjectName: String
)

// ✅ ADDED: Request body for inactivating a subject
@Serializable
data class UpdateRequirementStatusRequest(
    val status: String // "active" or "inactive"
)

@Serializable
data class UpdateOrderRequest(
    val orderedIds: List<Int>
)

@Serializable
data class ChangePasswordRequest(
    val userId: Int,
    val oldPassword: String,
    val newPassword: String
)

@Serializable
data class LogoutRequest(
    val userId: Int?,
    val username: String?
)

@Serializable
data class FCMTokenRequest(
    val userId: Int,
    val fcmToken: String
)

@Serializable
data class FullyClearedStudent(
    val userId: Int,         // Added to SQL query
    val studentId: String,   // Aliased in SQL
    val firstName: String,   // Aliased in SQL
    val middleName: String?, // Aliased in SQL
    val lastName: String,    // Aliased in SQL
    val gradeLevel: String,  // Aliased in SQL
    val sectionName: String  // Aliased in SQL (was returning 'section' before)
)

@Serializable
data class ReportsResponse(
    val totalCount: Int,     // Now provided by backend
    val students: List<FullyClearedStudent> // Now provided by backend
)