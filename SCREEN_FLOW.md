# Screen Flow Documentation

This document outlines the screen navigation flow for the different user roles in the application.

## 1. Student User

The student user has the simplest navigation flow.

- **`LoginScreen`**: The user logs in with their student credentials.
- **`StudentDetailScreen`**: After a successful login, the student is taken directly to their details screen, which displays their clearance status and other relevant information.

**Flow:**
`LoginScreen` -> `StudentDetailScreen`

## 2. Admin User

The admin user has access to a comprehensive dashboard with multiple management sections.

- **`LoginScreen`**: The user logs in with their admin credentials.
- **`AdminDashboard`**: After a successful login, the admin is taken to the main dashboard. This screen contains a bottom navigation bar with five tabs:
    - **Students**: Navigates to the `StudentManagementScreen`.
    - **Sections**: Navigates to the `SectionManagementScreen`.
    - **Signatories**: Navigates to the `SignatoryListScreen`.
    - **Subjects**: Navigates to the `CurriculumHomeScreen`.
    - **Accounts**: Navigates to the `AccountListScreen`.

From these main management screens, the admin can navigate to various other screens to perform actions like adding, editing, or viewing details.

**Example Flow (Students Tab):**
`LoginScreen` -> `AdminDashboard` -> `StudentManagementScreen` -> `AddStudentScreen`
`LoginScreen` -> `AdminDashboard` -> `StudentManagementScreen` -> `EditStudentScreen`
`LoginScreen` -> `AdminDashboard` -> `StudentManagementScreen` -> `AdminStudentDetailScreen`

## 3. Signatory User

The signatory user is responsible for managing clearance for specific subjects or accounts.

- **`LoginScreen`**: The user logs in with their signatory credentials.
- **`SignatoryDashboard`**: After a successful login, the signatory is taken to their dashboard. This screen displays a list of subjects and accounts that have been assigned to them.
- **`AssignedSectionsScreen` / `AssignedSectionsForAccountScreen`**: From the dashboard, the signatory can select a subject or account to view the list of assigned sections.
- **`ClearanceScreen`**: From the assigned sections screen, the signatory can navigate to the clearance screen to manage the clearance status of students for a specific subject or account in that section.

**Flow:**
`LoginScreen` -> `SignatoryDashboard` -> `AssignedSectionsScreen` -> `ClearanceScreen`
`LoginScreen` -> `SignatoryDashboard` -> `AssignedSectionsForAccountScreen` -> `ClearanceScreen`
