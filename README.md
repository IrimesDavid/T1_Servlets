## CSV Data Cleaning and Quality Analysis Platform

The project is a web-based application that analyzes CSV files, identifies data quality issues, generates a detailed report, and produces a cleaned version of the original dataset. It aims to automate common data cleaning tasks while allowing users to inspect detected problems and understand the transformations applied to their data.

### Architecture and Technologies

The application consists of three independent Java Servlet-based services, deployed and executed simultaneously in three different servlet containers: **Apache Tomcat, Eclipse Jetty, and WildFly**. The services communicate through HTTP requests, while a shared Docker volume is used to exchange intermediate files without transferring potentially large datasets in HTTP request bodies.

1. **Gateway Service — Apache Tomcat:** Provides the web interface and REST-style HTTP endpoints. It receives CSV uploads, validates them, assigns a unique job ID, stores the original file. It orchestrates the other services, exposes the analysis report to the frontend, send the corrected CSV back to the frontend on download request.

2. **Data Analysis Service — Eclipse Jetty:** Reads the original dataset from the shared volume and performs data quality checks. These include detecting missing values, inconsistent data types, duplicate rows, statistical outliers, and values outside user-defined ranges. It generates a structured JSON report containing the detected issues, their locations, severity, and suggested cleaning actions.

3. **Data Cleaning Service — WildFly:** Reads the original dataset and the analysis report from the shared volume. Based on predefined cleaning policies, it applies the selected transformations, such as trimming whitespace, normalizing missing values, and removing duplicate rows. Potentially ambiguous issues, such as outliers or invalid values, are handled according to explicit policies rather than automatically modifying data without justification. The service stores the cleaned dataset as CSV.

### Data Flow and Storage

The user uploads a CSV file through the web interface. The Gateway Service stores the original file on the shared volume, then invokes the Data Analysis Service through HTTP. After the analysis report has been generated, the gateway invokes the Data Cleaning Service, which uses both the original data and the report to produce the cleaned dataset. Once thats done too, the Gateway servlet sends a response with the report in JSON format, having a separate endpoint for downloading the corrected CSV.

The original CSV, analysis report, and cleaned dataset are stored as separate files associated with the job ID. HTTP is used for service orchestration, and the shared volume provides file-based data exchange.

The frontend interprets the response JSON (which also contains the job id, very important, because the frontend will keep that in mind so to speak) and displays a summary of the detected issues, including their categories, locations, and recommended actions, and allows users to download the cleaned CSV file by unlocking a download button (its locked until the uploaded CSV is analyzed). Finally, there is a \clear endpoint (with the job id parameter), that is activated by a Clear button in the frontend (which replaced the upload button once the analyze button was clicked previously). What it does is it clears the job with that id from the volume (the report & the original and corrected csv files) and the upload button shows up again instead of the Clear button on the frontend.

### Scope and Testing

The initial implementation focuses on CSV files with headers and a defined set of deterministic, configurable data quality rules. Statistical outlier detection is treated as a heuristic rather than proof that a value is incorrect. Each analysis rule and cleaning operation will be tested against predefined datasets with known issues and expected results. Integration and end-to-end tests will verify communication between the services, report correctness, cleaning behavior, and the final downloadable CSV.

We also need to implement and test how the system would behave when one or more servlets are down.

Possible future additions: SSE conncetion between the gateway servlet and frontend for live notification like (Analyzing... when the system waits for Jetty servlet and Cleaning... when WidFly does its thing).

The project demonstrates Java Servlets, HTTP-based communication between independently deployed services, Docker-based deployment, shared-volume storage, structured JSON processing, and automated data quality analysis and cleaning.