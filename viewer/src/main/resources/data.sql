INSERT INTO Clients (TitlePrefix, FirstName, MiddleName, LastName, TitleSuffix, Citizenship1, Citizenship2, OnboardingDate, Status, NameAtBirth, NickName, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus) VALUES 
('Mr.', 'Acme', 'The', 'Corp', '', 'USA', '', '2023-01-15', 'Active', 'Acme Senior', 'Junior', 'Male', '1980-05-20', 'English', 'Engineer', 'USA', 'USA', 'Reportable', 'Reportable'),
('Dr.', 'Globex', '', 'Inc', 'Esq.', 'UK', 'USA', '2023-03-22', 'Pending', 'Globex Alpha', 'G', 'Female', '1975-11-10', 'English', 'Scientist', 'UK', 'USA', 'Non-Reportable', 'Reportable'),
('', 'Soylent', 'Green', 'Corp', '', 'France', '', '2023-06-10', 'Inactive', 'Soylent Original', 'Greenie', 'Other', '1990-01-01', 'French', 'Analyst', 'France', 'France', 'Reportable', 'Non-Reportable'),
('Ms.', 'Umbrella', 'T', 'Corp', '', 'Canada', 'UK', '2023-11-05', 'Active', 'Umbrella Prototype', 'U', 'Female', '1985-08-15', 'English', 'Manager', 'Canada', 'UK', 'Non-Reportable', 'Non-Reportable');

INSERT INTO ClientAddresses (ClientID, AddressType, AddressLine1, AddressLine2, City, Zip, Country, AddressNumber, AddressSupplement) VALUES (1, 'Postal', '123 Road Runner Way', '', 'Desert City', '85001', 'USA', '123', 'Ground Floor');
INSERT INTO ClientAddresses (ClientID, AddressType, AddressLine1, AddressLine2, City, Zip, Country, AddressNumber, AddressSupplement) VALUES (2, 'Residential', '456 Cypress Creek', 'Suite 200', 'Springfield', '62704', 'USA', '456', 'Annex A');
INSERT INTO ClientAddresses (ClientID, AddressType, AddressLine1, AddressLine2, City, Zip, Country, AddressNumber, AddressSupplement) VALUES (3, 'Postal', '789 People Place', '', 'New York', '10001', 'USA', '789', '');
INSERT INTO ClientAddresses (ClientID, AddressType, AddressLine1, AddressLine2, City, Zip, Country, AddressNumber, AddressSupplement) VALUES (4, 'Other', '666 Hive St', 'Underground', 'Raccoon City', '90210', 'USA', '666', 'Level -5');


INSERT INTO ClientIdentifiers (ClientID, IdentifierType, IdentifierValue, IssuingAuthority, IdentifierNumber) VALUES (1, 'Passport', 'A12345678', 'USA Dept of State', 'P-123-456');
INSERT INTO ClientIdentifiers (ClientID, IdentifierType, IdentifierValue, IssuingAuthority, IdentifierNumber) VALUES (1, 'Social Issuance Number', '999-00-1111', 'SSA', 'S-999-00');
INSERT INTO ClientIdentifiers (ClientID, IdentifierType, IdentifierValue, IssuingAuthority, IdentifierNumber) VALUES (2, 'NID', 'NID-987654321', 'Springfield Gov', 'N-987-654');
INSERT INTO ClientIdentifiers (ClientID, IdentifierType, IdentifierValue, IssuingAuthority) VALUES (3, 'Other', 'CORP-ID-555', 'State Registry');
INSERT INTO ClientIdentifiers (ClientID, IdentifierType, IdentifierValue, IssuingAuthority, IdentifierNumber) VALUES (3, 'Tax ID', 'TAX-789-012', 'French Revenue Service', 'T-789-012');
INSERT INTO ClientIdentifiers (ClientID, IdentifierType, IdentifierValue, IssuingAuthority, IdentifierNumber) VALUES (4, 'Business Reg', 'BR-456-789', 'Canada Business Bureau', 'B-456-789');

INSERT INTO RelatedParties (ClientID, RelationType, TitlePrefix, FirstName, MiddleName, LastName, TitleSuffix, Citizenship1, Citizenship2, OnboardingDate, Status, NameAtBirth, NickName, Gender, DateOfBirth, Language, Occupation, CountryOfTax, SourceOfFundsCountry, FATCAStatus, CRSStatus) 
VALUES (1, 'Legal representative', 'Ms.', 'Jane', 'Marie', 'Doe', '', 'USA', '', '2023-01-16', 'Active', 'Jane BirthName', 'J', 'Female', '1992-04-10', 'English', 'Lawyer', 'USA', 'USA', 'Non-Reportable', 'Non-Reportable');

INSERT INTO RelatedPartyAddresses (RelatedPartyID, AddressType, AddressLine1, AddressLine2, City, Zip, Country, AddressNumber, AddressSupplement) 
VALUES (1, 'Residential', '789 Lawyer Ln', 'Apartment 4B', 'Legal City', '90210', 'USA', '789/B', 'Directly opposite park');

INSERT INTO RelatedPartyIdentifiers (RelatedPartyID, IdentifierType, IdentifierValue, IssuingAuthority, IdentifierNumber) 
VALUES (1, 'License', 'L-1234567', 'State Bar', 'LB-765-4321');

INSERT INTO Accounts (ClientID, AccountNumber, AccountStatus) VALUES (1, 'ACC-123456', 'Active');
INSERT INTO Accounts (ClientID, AccountNumber, AccountStatus) VALUES (1, 'SAV-999888', 'Inactive');
INSERT INTO Accounts (ClientID, AccountNumber, AccountStatus) VALUES (2, 'ACC-555444', 'Active');
INSERT INTO Accounts (ClientID, AccountNumber, AccountStatus) VALUES (3, 'ACC-000111', 'Pending');



INSERT INTO MaterialChanges (ChangeDate, ClientID, EntityID, EntityName, ColumnName, OperationType, OldValue, NewValue) VALUES
('2025-01-10 10:00:00', 1, 1, 'Client', 'Status', 'UPDATE', 'PENDING', 'ACTIVE'),
('2025-01-11 11:30:00', 1, 1, 'Address', 'Street', 'UPDATE', '123 Fake St', '124 Fake St'),
('2025-01-12 09:15:00', 2, 2, 'Client', 'LastName', 'UPDATE', 'Smith', 'Smith-Jones'),
('2025-01-13 14:20:00', 3, 3, 'Identifier', 'IDValue', 'UPDATE', 'A1234568', 'A9999999');





INSERT INTO Cases (ClientID, Reason, AssignedTo, Status) VALUES
(1, 'New Onboarding', 'analyst', 'KYC_ANALYST'),
(2, 'Periodic Review', 'reviewer', 'KYC_REVIEWER');

INSERT INTO CaseComments (CaseID, UserID, CommentText, Role) VALUES
(1, 'analyst', 'Starting onboarding for Acme Corp.', 'KYC_ANALYST'),
(2, 'analyst', 'Reviewing Globex Inc documents.', 'KYC_ANALYST'),
(2, 'reviewer', 'Documents look valid, passing to AFC.', 'KYC_REVIEWER');

-- Questionnaire Template
INSERT INTO QuestionnaireSections (SectionName, DisplayOrder) VALUES 
('Customer Identity', 1),
('Source of Wealth', 2),
('Risk Assessment', 3);

INSERT INTO QuestionnaireQuestions (SectionID, QuestionText, QuestionType, IsMandatory, Options, DisplayOrder) VALUES 
(1, 'Has the secondary ID been verified?', 'YES_NO', true, '', 1),
(1, 'Residential address confirmation date?', 'TEXT', true, '', 2),
(2, 'Main source of wealth?', 'MULTI_CHOICE', true, 'Salary,Inheritance,Investment,Other', 1),
(3, 'Is the customer a PEP (Politically Exposed Person)?', 'YES_NO', true, '', 1),
(3, 'Additional risk comments', 'TEXT', false, '', 2);

-- Sample Responses for Case 1
INSERT INTO CaseQuestionnaireResponses (CaseID, QuestionID, AnswerText) VALUES 
(1, 1, 'Yes'),
(1, 2, '2023-10-01'),
(1, 3, 'Salary');

INSERT INTO Portfolios (ClientID, AccountNumber, PortfolioText, OnboardingDate, OffboardingDate, Status) VALUES
(1, 'ACC-1001', 'Global Equities Portfolio', '2023-02-01', NULL, 'Active'),
(1, 'ACC-1002', 'Real Estate Fund', '2023-05-15', '2023-12-31', 'Closed'),
(2, 'ACC-2001', 'Fixed Income Alpha', '2023-04-01', NULL, 'Active'),
(3, 'ACC-3001', 'Venture Capital Tech', '2023-07-20', NULL, 'Active');

