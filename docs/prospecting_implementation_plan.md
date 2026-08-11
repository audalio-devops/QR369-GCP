# Prospecting Module Implementation Plan

This document outlines the implementation plan for the prospecting module within the `@/prospecting-service`.

## Sprint 1 - Backend

**Goal:** Create an endpoint to read data from the "Lista de Contatos" Google Sheet.

**Tasks:**

1.  **Create a new endpoint:** `GET /read-file`
    *   This endpoint will be responsible for reading the content of the "Lista de Contatos" Google Sheet.
    *   The sheet contains contact numbers and names for sending messages.
    *   This endpoint will be used to display the file content on the frontend and to fetch contacts for prospecting.

2.  **Integrate Google Sheets API:**
    *   Add the necessary dependencies to `pom.xml` for Google API Client and Google Auth Library.
    *   Use the provided code to access the Google Sheet as an editor.
    *   The service will read the data from the spreadsheet with ID `1OBNHijTLOqMOPJVSjutVSDJqBfGDF8jxbEOPEHJEp00`.
    *   The credentials will be loaded from `src/main/resources/credentials/qr369tools.json`.

**Dependencies to add to `pom.xml`:**

```xml
<!-- Google API Client para Sheets -->
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-sheets</artifactId>
    <version>v4-rev614-1.18.0-rc</version>
</dependency>
<!-- Google Auth Library para autenticação via Service Account -->
<dependency>
    <groupId>com.google.auth</groupId>
    <artifactId>google-auth-library-oauth2-http</artifactId>
    <version>1.23.0</version>
</dependency>
```

## Sprint 2 - Frontend

**Goal:** Create the user interface for the prospecting module.

**Tasks:**

1.  **Create the prospecting screen:**
    *   The screen will have a list component to display the information from the "Lista de Contatos" spreadsheet (Contact, Contact No., Date Contacted, Status).
    *   Include a button "Iniciar Prospecção".

2.  **Implement "Iniciar Prospecção" button logic:**
    *   When clicked, the button should ask the user for confirmation to start prospecting.
    *   If the user confirms, it will trigger the `/prospecting` endpoint (to be created in Sprint 3) and show a popup: "Prospecção Iniciada às [date and time]".
    *   If the user cancels, it will show the message: "Prospecção cancelada".

**Validation:**
*   The prospecting module screen in the QR369 Tools application should display the content of the "Lista de Contatos" spreadsheet.
*   Clicking the "Iniciar Prospecção" button should show the confirmation prompt and the corresponding confirmation or cancellation message.

## Sprint 3 - Backend

**Goal:** Implement the core prospecting logic to send messages.

**Tasks:**

1.  **Create a new endpoint:** `POST /prospecting`
    *   This endpoint will send a message to each contact in the "Lista de Contatos" sheet.

2.  **Implement message sending logic:**
    *   For each contact, a random number between 1 and 5 will be chosen.
    *   The message corresponding to the random number will be fetched from the `prospecting-service/src/main/resources/messages/msg_X.st` file, where X is the chosen number.
    *   Use the `br.com.ia369.virtual_assistant.whatsapp` package to send the message via Z-API.
    *   The frequency of sending messages should be between 15 to 20 minutes, and only until 6 PM.
    *   **Note:** The actual sending of the message via the Z-API should be disabled for now as the API is not active.

3.  **Log prospecting activity:**
    *   After each prospecting attempt, record the date and time the message was sent and its status.
    *   This information should be updated in the Google Sheet for the corresponding contact.
