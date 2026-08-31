-- Sample data for chatbot_category table
-- Context: Interview Candidate Site FAQs

-- Note: The 'created_by', and 'modified_by' columns expect UUIDs. 
-- The 'created_date' and 'modified_date' are often handled by the database/ORM.
-- The following queries assume a PostgreSQL database where NOW() can be used for timestamps.
-- The 'id' column is auto-generated and is not included in the INSERT statements.

-- Parent Category: General
INSERT INTO chatbot_category (parent_category, category_name, question, answer, created_by, modified_by, is_active, created_date, modified_date)
VALUES
('General', 'Interview Process', 'What is the interview process?', 'The interview process consists of three rounds: a technical screening, a coding challenge, and a final behavioral interview.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('General', 'Interview Process', 'How should I prepare for the technical interview?', 'We recommend reviewing data structures, algorithms, and system design concepts. Familiarity with our tech stack is a plus.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW());

-- Parent Category: Technical
INSERT INTO chatbot_category (parent_category, category_name, question, answer, created_by, modified_by, is_active, created_date, modified_date)
VALUES
('Technical', 'Tech Stack', 'What is your tech stack?', 'We primarily use Java with the Spring Framework for our backend, and React for our frontend. We use PostgreSQL for our database and our services are deployed on AWS.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Technical', 'Coding Challenge', 'Do you have a coding challenge?', 'Yes, we have a coding challenge that you can complete in your own time. It usually takes 2-3 hours.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW());

-- Parent Category: Logistics
INSERT INTO chatbot_category (parent_category, category_name, question, answer, created_by, modified_by, is_active, created_date, modified_date)
VALUES
('Logistics', 'Office Location', 'Where are your offices located?', 'Our main office is in San Francisco, but we have smaller offices in New York and London. We also offer remote work options.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Logistics', 'Work Hours', 'What are the working hours?', 'We have flexible working hours. Our core hours are from 10 AM to 4 PM in your local time zone.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW());

-- Parent Category: General (Continued)
INSERT INTO chatbot_category (parent_category, category_name, question, answer, created_by, modified_by, is_active, created_date, modified_date)
VALUES
('General', 'Interview Process', 'How long does the entire interview process take?', 'Typically, the process takes 2-3 weeks from the initial screening to the final offer.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('General', 'Interview Process', 'Who will I be interviewing with?', 'You will meet with a mix of team members, including individual contributors, a team lead, and a manager.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('General', 'Application Status', 'How can I check my application status?', 'You can check your application status through the portal you used to apply. You will also receive email updates.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('General', 'Feedback', 'Will I receive feedback on my interview?', 'Yes, we provide feedback to all candidates who complete the final interview round.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW());

-- Parent Category: Technical (Continued)
INSERT INTO chatbot_category (parent_category, category_name, question, answer, created_by, modified_by, is_active, created_date, modified_date)
VALUES
('Technical', 'Coding Challenge', 'What programming languages can I use for the coding challenge?', 'You can use any of the following languages: Java, Python, JavaScript, or C++.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Technical', 'System Design', 'Is there a system design round?', 'Yes, for senior roles, there is a system design round where you will be asked to design a scalable system.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Technical', 'Pair Programming', 'Do you do pair programming in interviews?', 'Some of our technical interviews may involve a pair programming session with one of our engineers.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Technical', 'Open Source', 'Do you contribute to open source?', 'Yes, we encourage our engineers to contribute to open source projects and we have several of our own.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW());

-- Parent Category: Logistics (Continued)
INSERT INTO chatbot_category (parent_category, category_name, question, answer, created_by, modified_by, is_active, created_date, modified_date)
VALUES
('Logistics', 'Remote Work', 'What is your remote work policy?', 'We are a remote-first company, but employees have the option to work from our offices if they prefer.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Logistics', 'Interview Scheduling', 'Can I reschedule my interview?', 'Yes, you can reschedule your interview by contacting your recruiter. Please give us at least 24 hours notice.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Logistics', 'Travel', 'Do you cover travel expenses for interviews?', 'For the final, on-site interview, we will cover all reasonable travel and accommodation expenses.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Logistics', 'Equipment', 'Do you provide equipment for remote employees?', 'Yes, we provide a laptop and a budget for other home office equipment.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW());

-- Parent Category: Benefits
INSERT INTO chatbot_category (parent_category, category_name, question, answer, created_by, modified_by, is_active, created_date, modified_date)
VALUES
('Benefits', 'Health Insurance', 'What health insurance do you offer?', 'We offer a comprehensive health insurance plan that includes medical, dental, and vision coverage.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Benefits', 'Vacation', 'What is your vacation policy?', 'We offer unlimited paid time off. We encourage employees to take at least 3 weeks off per year.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Benefits', 'Retirement', 'Do you have a retirement plan?', 'Yes, we offer a 401(k) plan with a 4% company match.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Benefits', 'Professional Development', 'Do you offer professional development opportunities?', 'Yes, we provide a yearly stipend for conferences, courses, and books.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW());

-- Parent Category: Company Culture
INSERT INTO chatbot_category (parent_category, category_name, question, answer, created_by, modified_by, is_active, created_date, modified_date)
VALUES
('Company Culture', 'Team Structure', 'What is the team structure like?', 'We work in small, agile teams of 5-7 people. Each team has a mix of frontend and backend engineers, a product manager, and a designer.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Company Culture', 'Work-Life Balance', 'How is the work-life balance?', 'We value work-life balance and have a flexible work policy. We discourage working on weekends.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Company Culture', 'Diversity and Inclusion', 'What are your diversity and inclusion initiatives?', 'We have several employee resource groups and we are committed to building a diverse and inclusive workplace. We publish an annual diversity report.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('Company Culture', 'Social Events', 'Are there social events?', 'Yes, we have regular team lunches, happy hours, and company-wide events throughout the year.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW());

-- Additional FAQs
INSERT INTO chatbot_category (parent_category, category_name, question, answer, created_by, modified_by, is_active, created_date, modified_date)
VALUES
('General', 'FAQ', 'How long does the hiring process take?', 'Typically it takes 2-4 weeks from application to final decision, depending on the role and number of applicants.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('General', 'FAQ', 'How can I check my application status?', 'You can check application status on the ''Applied Jobs'' page or ask the chat: ''status of application.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('General', 'FAQ', 'What documents are required?', 'You must upload your resume and a government-issued ID. Some roles may require additional certificates.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('General', 'FAQ', 'Can I apply to multiple roles?', 'Yes — you can apply to multiple roles. Make sure your resume and profile match each role''s requirements and tailor your application where needed.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW()),
('General', 'FAQ', 'How do I apply for a job?', 'To apply, open the job details page and click the ''Apply'' button. Upload your resume, fill the required fields, and submit.', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', true, NOW(), NOW());
