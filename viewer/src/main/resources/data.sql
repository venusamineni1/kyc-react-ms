INSERT INTO Users (Username, Password, Role, Active) VALUES ('admin', 'admin', 'ADMIN', true);
INSERT INTO Users (Username, Password, Role, Active) VALUES ('analyst', 'password', 'KYC_ANALYST', true);
INSERT INTO Users (Username, Password, Role, Active) VALUES ('reviewer', 'password', 'KYC_REVIEWER', true);

INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'MANAGE_USERS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'MANAGE_PERMISSIONS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'MANAGE_CASES');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'VIEW_CLIENTS');
INSERT INTO RolePermissions (Role, Permission) VALUES ('ADMIN', 'VIEW_CHANGES');
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_ANALYST', 'MANAGE_CASES');
INSERT INTO RolePermissions (Role, Permission) VALUES ('KYC_ANALYST', 'VIEW_CHANGES');
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

-- Additional Test Clients
-- 3. Corporate: Global Tech Solutions (DEU)
INSERT INTO Clients (TitlePrefix, FirstName, LastName, OnboardingDate, Status, Citizenship1, Citizenship2, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus)
VALUES (NULL, 'Global Tech', 'Solutions', '2023-05-12', 'ACTIVE', 'DEU', NULL, NULL, NULL, 'German', 'Technology', 'DEU', 'DEU', 'Active NFFE', 'Active NFE');
INSERT INTO ClientAddresses (ClientID, AddressType, AddressLine1, City, Zip, Country)
VALUES (3, 'Business', 'Friedrichstraße 12', 'Berlin', '10117', 'DEU');
INSERT INTO ClientIdentifiers (ClientID, IdentifierType, IdentifierValue, IssuingAuthority)
VALUES (3, 'LEI', '1234567890ABCDEFGHIJ', 'GLEIF');

-- 4. Individual: Elena Rodriguez (ESP) - High Risk profile
INSERT INTO Clients (TitlePrefix, FirstName, LastName, OnboardingDate, Status, Citizenship1, Citizenship2, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus)
VALUES ('Ms', 'Elena', 'Rodriguez', '2023-08-20', 'SUSPENDED', 'ESP', 'MEX', 'Female', '1975-11-03', 'Spanish', 'Art Dealer', 'ESP', 'MEX', 'Reportable', 'Reportable');
INSERT INTO ClientAddresses (ClientID, AddressType, AddressLine1, City, Zip, Country)
VALUES (4, 'Residential', 'Calle de Alcalá 45', 'Madrid', '28014', 'ESP');
INSERT INTO ClientIdentifiers (ClientID, IdentifierType, IdentifierValue, IssuingAuthority)
VALUES (4, 'DNI', '12345678Z', 'Ministerio del Interior');

-- 5. Individual: Yuki Tanaka (JPN)
INSERT INTO Clients (TitlePrefix, FirstName, LastName, OnboardingDate, Status, Citizenship1, Citizenship2, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus)
VALUES ('Mr', 'Yuki', 'Tanaka', '2024-01-05', 'IN_REVIEW', 'JPN', NULL, 'Male', '1992-04-12', 'Japanese', 'Software Developer', 'JPN', 'JPN', 'Reportable', 'Reportable');
INSERT INTO ClientAddresses (ClientID, AddressType, AddressLine1, City, Zip, Country)
VALUES (5, 'Residential', '1-2-1 Shibaura', 'Tokyo', '105-0023', 'JPN');

-- 6. Individual: Sarah Al-Farsi (ARE)
INSERT INTO Clients (TitlePrefix, FirstName, LastName, OnboardingDate, Status, Citizenship1, Citizenship2, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus)
VALUES ('Ms', 'Sarah', 'Al-Farsi', '2024-02-14', 'ACTIVE', 'ARE', NULL, 'Female', '1988-09-30', 'Arabic', 'Financial Analyst', 'ARE', 'ARE', 'Reportable', 'Reportable');
INSERT INTO ClientAddresses (ClientID, AddressType, AddressLine1, City, Zip, Country)
VALUES (6, 'Residential', 'Dubai Marina Tower 1', 'Dubai', '00000', 'ARE');

-- 7. Corporate: Nordic Innovations (FIN)
INSERT INTO Clients (TitlePrefix, FirstName, LastName, OnboardingDate, Status, Citizenship1, Citizenship2, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus)
VALUES (NULL, 'Nordic', 'Innovations', '2024-02-28', 'ACTIVE', 'FIN', NULL, NULL, NULL, 'Finnish', 'Renewable Energy', 'FIN', 'FIN', 'Active NFFE', 'Active NFE');

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
-- Material Change Configs
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Client', 'citizenship1', 'BOTH');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Client', 'citizenship2', 'BOTH');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Client', 'occupation', 'RISK');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Client', 'countryOfTax', 'RISK');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Client', 'sourceOfFundsCountry', 'RISK');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Client', 'fatcaStatus', 'RISK');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Client', 'crsStatus', 'RISK');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Client', 'firstName', 'SCREENING');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Client', 'lastName', 'SCREENING');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Client', 'dateOfBirth', 'BOTH');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Address', 'country', 'RISK');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Address', 'city', 'RISK');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Address', 'ALL', 'RISK');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Identifier', 'identifierNumber', 'SCREENING');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('Identifier', 'ALL', 'SCREENING');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('RelatedParty', 'relationType', 'RISK');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('RelatedParty', 'citizenship1', 'BOTH');
INSERT INTO MaterialChangeConfigs (EntityName, ColumnName, Category) VALUES ('RelatedParty', 'ALL', 'SCREENING');

-- 101. Corporate: Global Corp (SGP)
INSERT INTO Clients (ClientID, TitlePrefix, FirstName, LastName, OnboardingDate, Status, Citizenship1, Citizenship2, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus)
VALUES (101, NULL, 'Global', 'Corp', '2024-03-01', 'ACTIVE', 'SGP', NULL, NULL, NULL, 'Logistics', 'SGP', 'SGP', 'Active NFFE', 'Active NFE');
INSERT INTO ClientAddresses (ClientID, AddressType, AddressLine1, City, Zip, Country)
VALUES (101, 'Business', 'Marina Bay Financial Centre', 'Singapore', '018981', 'SGP');
INSERT INTO ClientIdentifiers (ClientID, IdentifierType, IdentifierValue, IssuingAuthority)
VALUES (101, 'UEN', '202410101G', 'ACRA');

-- 102. Individual: Amara Okafor (NGA)
INSERT INTO Clients (ClientID, TitlePrefix, FirstName, LastName, OnboardingDate, Status, Citizenship1, Citizenship2, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus)
VALUES (102, 'Ms', 'Amara', 'Okafor', '2024-03-02', 'ACTIVE', 'NGA', NULL, 'Female', '1995-07-22', 'English', 'Doctor', 'NGA', 'NGA', 'Reportable', 'Reportable');
INSERT INTO ClientAddresses (ClientID, AddressType, AddressLine1, City, Zip, Country)
VALUES (102, 'Residential', '15 Victoria Island', 'Lagos', '101241', 'NGA');

-- 103. Individual: Liam O''Connor (IRL)
INSERT INTO Clients (ClientID, TitlePrefix, FirstName, LastName, OnboardingDate, Status, Citizenship1, Citizenship2, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus)
VALUES (103, 'Mr', 'Liam', 'O''Connor', '2024-03-03', 'IN_REVIEW', 'IRL', NULL, 'Male', '1982-11-15', 'English', 'Writer', 'IRL', 'IRL', 'Reportable', 'Reportable');

-- 104. Corporate: TechFlow Systems (USA)
INSERT INTO Clients (ClientID, TitlePrefix, FirstName, LastName, OnboardingDate, Status, Citizenship1, Citizenship2, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus)
VALUES (104, NULL, 'TechFlow', 'Systems', '2024-03-04', 'ACTIVE', 'USA', NULL, NULL, NULL, 'Software', 'USA', 'USA', 'Active NFFE', 'Active NFE');

-- 105. Individual: Carlos Silva (BRA)
INSERT INTO Clients (ClientID, TitlePrefix, FirstName, LastName, OnboardingDate, Status, Citizenship1, Citizenship2, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus)
VALUES (105, 'Mr', 'Carlos', 'Silva', '2024-03-05', 'SUSPENDED', 'BRA', NULL, 'Male', '1970-01-20', 'Portuguese', 'Business Owner', 'BRA', 'BRA', 'Reportable', 'Reportable');
