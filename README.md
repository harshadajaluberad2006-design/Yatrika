# Yatrika - Travel Website

Yatrika is a travel website built using HTML, CSS, JavaScript, Java, and MySQL.

## Features
- Explore travel destinations and packages.
- Browse adventure travel options.
- Submit travel enquiries through a contact form.
- Store enquiries in a MySQL database.
- View enquiries through an admin dashboard.
- Validate form submissions in the Java backend.
- Protect the admin API with token-based authentication.

## Technologies
- HTML5
- CSS3
- JavaScript
- Java
- MySQL
- JDBC
- Java HTTP Server
- MySQL Connector/J

## Prerequisites
- Java JDK
- MySQL Server
- MySQL command-line client

## Database Setup

Run this command from the project directory:

    mysql -u root -p < .\database\schema.sql

Enter your MySQL password when prompted.

## Configure Environment Variables

Set these variables in PowerShell, replacing the example values with your own:

    $env:YATRIKA_DB_USER = "root"
    $env:YATRIKA_DB_PASSWORD = "YOUR_MYSQL_PASSWORD"
    $env:YATRIKA_ADMIN_TOKEN = "YOUR_PRIVATE_ADMIN_TOKEN"

Never publish real passwords or admin tokens on GitHub.

## Compile the Java Server

    javac -cp ".;lib/*" YatrikaServer.java

## Start the Server

    java -cp ".;lib/*" YatrikaServer

Open http://localhost:8080 in your browser.

## Website Pages

- Home: http://localhost:8080/
- Destinations: http://localhost:8080/destinations.html
- Adventures: http://localhost:8080/adventures.html
- Packages: http://localhost:8080/packages.html
- About: http://localhost:8080/about.html
- Testimonials: http://localhost:8080/testimonials.html
- Contact: http://localhost:8080/contact.html
- Admin dashboard: http://localhost:8080/admin.html
- Health check: http://localhost:8080/health

## Admin API

Endpoint: GET /api/admin/enquiries

The endpoint requires the X-Admin-Token request header. Keep the admin token private.

## Security Notes

- Keep MySQL running while using database features.
- The server uses port 8080 by default.
- This project is configured for local development.
- Configure HTTPS, production database credentials, CORS, and additional security controls before deployment.

## Future Improvements
- Add automated backend tests.
- Improve mobile responsiveness and accessibility.
- Add destination search and filtering.
- Deploy the application to a hosting platform.

## Author

Developed as a Java full-stack project using HTML, CSS, JavaScript, Java, JDBC, and MySQL.
