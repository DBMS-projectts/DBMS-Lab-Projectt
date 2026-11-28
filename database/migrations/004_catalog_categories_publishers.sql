USE lims_db;

-- Add catalogue categories without duplicating existing rows.
INSERT INTO categories (category_name, description)
SELECT 'Software Engineering', 'Software design, development, testing and maintenance'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Software Engineering');

INSERT INTO categories (category_name, description)
SELECT 'Database Management', 'Database systems, SQL, data modelling and administration'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Database Management');

INSERT INTO categories (category_name, description)
SELECT 'Artificial Intelligence', 'Artificial intelligence concepts and applications'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Artificial Intelligence');

INSERT INTO categories (category_name, description)
SELECT 'Machine Learning', 'Machine learning methods and practical applications'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Machine Learning');

INSERT INTO categories (category_name, description)
SELECT 'Web Development', 'Frontend, backend and full-stack web development'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Web Development');

INSERT INTO categories (category_name, description)
SELECT 'Networking', 'Computer networks and communication systems'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Networking');

INSERT INTO categories (category_name, description)
SELECT 'Cyber Security', 'Information security, privacy and ethical hacking'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Cyber Security');

INSERT INTO categories (category_name, description)
SELECT 'Physics', 'Classical and modern physics'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Physics');

INSERT INTO categories (category_name, description)
SELECT 'Chemistry', 'General, organic and applied chemistry'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Chemistry');

INSERT INTO categories (category_name, description)
SELECT 'History', 'World, regional and cultural history'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'History');

INSERT INTO categories (category_name, description)
SELECT 'Islamic Studies', 'Islamic history, culture and scholarship'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Islamic Studies');

INSERT INTO categories (category_name, description)
SELECT 'Business Studies', 'Business, management, finance and economics'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = 'Business Studies');

-- Add commonly used academic and technical publishers without duplicates.
INSERT INTO publishers (publisher_name)
SELECT 'Pearson'
WHERE NOT EXISTS (SELECT 1 FROM publishers WHERE publisher_name = 'Pearson');

INSERT INTO publishers (publisher_name)
SELECT 'McGraw Hill'
WHERE NOT EXISTS (SELECT 1 FROM publishers WHERE publisher_name = 'McGraw Hill');

INSERT INTO publishers (publisher_name)
SELECT 'Oxford University Press'
WHERE NOT EXISTS (SELECT 1 FROM publishers WHERE publisher_name = 'Oxford University Press');

INSERT INTO publishers (publisher_name)
SELECT 'Cambridge University Press'
WHERE NOT EXISTS (SELECT 1 FROM publishers WHERE publisher_name = 'Cambridge University Press');

INSERT INTO publishers (publisher_name)
SELECT 'Wiley'
WHERE NOT EXISTS (SELECT 1 FROM publishers WHERE publisher_name = 'Wiley');

INSERT INTO publishers (publisher_name)
SELECT 'Springer Nature'
WHERE NOT EXISTS (SELECT 1 FROM publishers WHERE publisher_name = 'Springer Nature');

INSERT INTO publishers (publisher_name)
SELECT 'Packt Publishing'
WHERE NOT EXISTS (SELECT 1 FROM publishers WHERE publisher_name = 'Packt Publishing');

INSERT INTO publishers (publisher_name)
SELECT 'O''Reilly Media'
WHERE NOT EXISTS (SELECT 1 FROM publishers WHERE publisher_name = 'O''Reilly Media');

INSERT INTO publishers (publisher_name)
SELECT 'Prentice Hall'
WHERE NOT EXISTS (SELECT 1 FROM publishers WHERE publisher_name = 'Prentice Hall');

INSERT INTO publishers (publisher_name)
SELECT 'CRC Press'
WHERE NOT EXISTS (SELECT 1 FROM publishers WHERE publisher_name = 'CRC Press');

INSERT INTO publishers (publisher_name)
SELECT 'Cengage'
WHERE NOT EXISTS (SELECT 1 FROM publishers WHERE publisher_name = 'Cengage');

INSERT INTO publishers (publisher_name)
SELECT 'MIT Press'
WHERE NOT EXISTS (SELECT 1 FROM publishers WHERE publisher_name = 'MIT Press');
