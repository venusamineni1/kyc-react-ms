INSERT INTO Users (Username, Password, Role, Active) VALUES ('admin', 'admin', 'ADMIN', true);
INSERT INTO Users (Username, Password, Role, Active) VALUES ('analyst', 'password', 'KYC_ANALYST', true);
INSERT INTO Users (Username, Password, Role, Active) VALUES ('reviewer', 'password', 'KYC_REVIEWER', true);

INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'MANAGE_USERS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'MANAGE_PERMISSIONS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'MANAGE_CASES');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'VIEW_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_ANALYST', 'MANAGE_CASES');
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_ANALYST', 'VIEW_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_REVIEWER', 'MANAGE_CASES');
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_REVIEWER', 'VIEW_CLIENTS');
INSERT INTO Users (Username, Password, Role, Active) VALUES ('afc_user', 'password', 'AFC_REVIEWER', true);
INSERT INTO Users (Username, Password, Role, Active) VALUES ('aco_user', 'password', 'ACO_REVIEWER', true);
INSERT INTO Users (Username, Password, Role, Active) VALUES ('auditor', 'password', 'AUDITOR', true);
INSERT INTO Users (Username, Password, Role, Active) VALUES ('analyst2', 'password', 'KYC_ANALYST', true);
INSERT INTO Users (Username, Password, Role, Active) VALUES ('reviewer2', 'password', 'KYC_REVIEWER', true);

INSERT INTO RolePermissions (Role, Permission) VALUES ('AFC_REVIEWER', 'MANAGE_CASES');
INSERT INTO RolePermissions (Role, Permission) VALUES ('AFC_REVIEWER', 'VIEW_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ACO_REVIEWER', 'MANAGE_CASES');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ACO_REVIEWER', 'VIEW_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('AUDITOR', 'VIEW_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('AUDITOR', 'VIEW_CHANGES');

INSERT INTO Clients (TitlePrefix, FirstName, LastName, OnboardingDate, Status, Citizenship1, Citizenship2, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus)
VALUES ('Mr', 'John', 'Doe', '2023-01-15', 'ACTIVE', 'USA', 'CAN', 'Male', '1980-05-20', 'English', 'Engineer', 'USA', 'USA', 'Reportable', 'Reportable');

INSERT INTO Clients (TitlePrefix, FirstName, LastName, OnboardingDate, Status, Citizenship1, Citizenship2, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus)
VALUES (NULL, 'Acme', 'Corp', '2023-02-10', 'ACTIVE', 'USA', NULL, NULL, NULL, 'English', 'Manufacturing', 'USA', 'USA', 'Active NFFE', 'Active NFE');

INSERT INTO ClientAddresses (ClientID, AddressType, AddressLine1, City, Zip, Country)
VALUES (1, 'Residential', '123 Main St', 'New York', '10001', 'USA');

INSERT INTO ClientAddresses (ClientID, AddressType, AddressLine1, City, Zip, Country)
VALUES (2, 'Business', '456 Tech Park', 'San Francisco', '94107', 'USA');

INSERT INTO ClientIdentifiers (ClientID, IdentifierType, IdentifierValue, IssuingAuthority)
VALUES (1, 'Passport', 'A12345678', 'USA Dept of State');

INSERT INTO ClientIdentifiers (ClientID, IdentifierType, IdentifierValue, IssuingAuthority)
VALUES (2, 'Tax ID', '98-7654321', 'IRS');

INSERT INTO RelatedParties (ClientID, RelationType, TitlePrefix, FirstName, LastName, Citizenship1, Status)
VALUES (2, 'Director', 'Ms', 'Jane', 'Smith', 'USA', 'ACTIVE');

INSERT INTO RelatedPartyAddresses (RelatedPartyID, AddressType, AddressLine1, City, Zip, Country)
VALUES (1, 'Residential', '789 Pine Ln', 'Austin', '73301', 'USA');

INSERT INTO RelatedPartyIdentifiers (RelatedPartyID, IdentifierType, IdentifierValue, IssuingAuthority)
VALUES (1, 'Passport', 'B98765432', 'USA Dept of State');

INSERT INTO Cases (ClientID, WorkflowType, Status, Reason, CreatedDate, AssignedTo)
VALUES (1, 'CMMN', 'KYC_ANALYST', 'New Onboarding', CURRENT_TIMESTAMP, 'analyst');

INSERT INTO Cases (ClientID, WorkflowType, Status, Reason, CreatedDate, AssignedTo)
VALUES (2, 'CMMN', 'KYC_ANALYST', 'Periodic Review', CURRENT_TIMESTAMP, 'analyst2');

-- Questionnaire Seeding
INSERT INTO QuestionnaireSections (SectionName, DisplayOrder) VALUES ('General Information', 1);
INSERT INTO QuestionnaireSections (SectionName, DisplayOrder) VALUES ('Risk Factors', 2);
INSERT INTO QuestionnaireSections (SectionName, DisplayOrder) VALUES ('Declarations', 3);

-- Questions for General Information (Section 1)
INSERT INTO QuestionnaireQuestions (SectionID, QuestionText, QuestionType, IsMandatory, DisplayOrder) 
VALUES (1, 'Is the customer a PEP?', 'YES_NO', true, 1);
INSERT INTO QuestionnaireQuestions (SectionID, QuestionText, QuestionType, IsMandatory, DisplayOrder) 
VALUES (1, 'Source of Wealth description', 'TEXT', true, 2);

-- Questions for Risk Factors (Section 2)
INSERT INTO QuestionnaireQuestions (SectionID, QuestionText, QuestionType, IsMandatory, DisplayOrder, RiskFactorKey) 
VALUES (2, 'Does the customer have adverse media?', 'YES_NO', true, 1, 'ADVERSE_MEDIA');
INSERT INTO QuestionnaireQuestions (SectionID, QuestionText, QuestionType, IsMandatory, DisplayOrder, Options) 
VALUES (2, 'Customer Risk Rating', 'MULTI_CHOICE', true, 2, 'Low,Medium,High');

-- Questions for Declarations (Section 3)
INSERT INTO QuestionnaireQuestions (SectionID, QuestionText, QuestionType, IsMandatory, DisplayOrder) 
VALUES (3, 'All documents verified?', 'YES_NO', true, 1);

-- Responses for Case 1 (John Doe)
INSERT INTO CaseQuestionnaireResponses (CaseID, QuestionID, AnswerText) VALUES (1, 1, 'No');
INSERT INTO CaseQuestionnaireResponses (CaseID, QuestionID, AnswerText) VALUES (1, 2, 'Employment income');
INSERT INTO CaseQuestionnaireResponses (CaseID, QuestionID, AnswerText) VALUES (1, 3, 'No');
INSERT INTO CaseQuestionnaireResponses (CaseID, QuestionID, AnswerText) VALUES (1, 4, 'Low');
INSERT INTO CaseQuestionnaireResponses (CaseID, QuestionID, AnswerText) VALUES (1, 5, 'Yes');

-- Responses for Case 2 (Acme Corp)
INSERT INTO CaseQuestionnaireResponses (CaseID, QuestionID, AnswerText) VALUES (2, 1, 'No');
INSERT INTO CaseQuestionnaireResponses (CaseID, QuestionID, AnswerText) VALUES (2, 2, 'Business operations');
INSERT INTO CaseQuestionnaireResponses (CaseID, QuestionID, AnswerText) VALUES (2, 3, 'No');
INSERT INTO CaseQuestionnaireResponses (CaseID, QuestionID, AnswerText) VALUES (2, 4, 'Medium');
INSERT INTO CaseQuestionnaireResponses (CaseID, QuestionID, AnswerText) VALUES (2, 5, 'Yes');

-- Missing Permissions for Risk and Screening
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_ANALYST', 'MANAGE_RISK');
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_ANALYST', 'MANAGE_SCREENING');
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_ANALYST', 'MANAGE_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_ANALYST', 'ROLE_ANALYST');

INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_REVIEWER', 'MANAGE_RISK');
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_REVIEWER', 'MANAGE_SCREENING');
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_REVIEWER', 'MANAGE_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_REVIEWER', 'ROLE_REVIEWER');

INSERT INTO RolePermissions (Role, Permission) VALUES ('AFC_REVIEWER', 'MANAGE_RISK');
INSERT INTO RolePermissions (Role, Permission) VALUES ('AFC_REVIEWER', 'MANAGE_SCREENING');
INSERT INTO RolePermissions (Role, Permission) VALUES ('AFC_REVIEWER', 'MANAGE_CLIENTS');

INSERT INTO RolePermissions (Role, Permission) VALUES ('ACO_REVIEWER', 'MANAGE_RISK');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ACO_REVIEWER', 'MANAGE_SCREENING');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ACO_REVIEWER', 'MANAGE_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ACO_REVIEWER', 'ROLE_ACO_REVIEWER');

-- Missing Permissions for ADMIN
INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'MANAGE_RISK');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'MANAGE_SCREENING');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'MANAGE_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'MANAGE_AUDITS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'MANAGE_CONFIG');

-- Requested Specific Roles
INSERT INTO RolePermissions (Role, Permission) VALUES ('ROLE_ANALYST', 'VIEW_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ROLE_ANALYST', 'MANAGE_CASES');

INSERT INTO RolePermissions (Role, Permission) VALUES ('ROLE_REVIEWER', 'VIEW_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ROLE_REVIEWER', 'MANAGE_CASES');

INSERT INTO RolePermissions (Role, Permission) VALUES ('ROLE_ACO_REVIEWER', 'VIEW_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ROLE_ACO_REVIEWER', 'MANAGE_CASES');
