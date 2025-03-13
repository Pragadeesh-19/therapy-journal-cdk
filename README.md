clr# Therapy Jounal API - EffDog Hiring Assignment

A serverless backend API for managing therapy Sessions, Appointments, client-therapist mappings, and messaging system using AWS lambda, API gateway and dynamoDB. 

## Table of contents
- [Introduction](#introduction)
- [Features](#features)
- [Technologies](#technologies)
- [API Design](#api-design)
- [Database Schema](#database-schema)
- [Implementation](#implementation)
- [Deployment](#deployment)
- [Testing](#testing)
- [Assumptions](#assumptions)

## Introduction

This project implements the backend APIs for a therapy journal management system as part of EffDog's hiring process. The solution follows serverless aechitecture best practices using AWS Lambda and Java. 

Key Requirements Implemented:
- Therapy Session CRUD operations
- Appointments management
- Client therapist Mapping management
- Journal Access request workflow
- Messaging System between Clients and Therapists

## Features
- **Therapy Sessions**
    - Create/Read/Update/Delete sessions
    - List sessions by therapist
- **Appointment**
    - create an appointment
- **Mappings**
    - Establish Client-Therapist relationships
    - Manage jounal access requests
- **Messaging**
    - Send messages between client and therapist
    - retrieve message history
 
## Technologies
- **AWS Services**
    - Lambda (Java 17)
    - API gateway
    - DynamoDB
    - CloudFormation (via CDK)
- **Frameworks**
    - AWS CDK (Infrastructure as code)
    - Swagger/OpenAPI 3.0
- **Tools**
    - Postman (API testing)
    - Maven (Build tool)

## API Design

The API specification is defined using OpenAPI 3.0. Key endpoints include: 

```yaml
# Simplified OpenAPI snippet
paths:
  /therapist/{therapistId}/sessions:
    POST:
      summary: Create therapy session
    GET:
      summary: List therapy Session

  /therapist/{therapistId}/sessions/{sessionId}:
    PUT:
      summary: Update therapy sessions
    DELETE:
      summary: Delete Therapy Session

  /sessions/{sessionId}/appointments:
    POST:
      summary: Request an appointment for a session slot

  /appointments/{appointmentId}:
    PUT:
      summary: Update appointment status

  /clients/{clientId}/mappings:
    GET:
      summary: Get mapping for a client (mappingId will be generated when this endpoint is called)

  /client-therapist-mappings/{mappingId}/journal-access-request:
    POST:
      summary: Request access to clients journal access

  /client-therapist-mappings/{mappingId}:
    PUT:
      summary: Update Journal access

  /messages:
    POST:
      summary: Send a message
    GET:
      summary: Get Message history
```

## Database schema

### Sessions Table
**Table Name:** `Sessions`  
**Primary Key:**  
- Partition Key: `therapistId`  
- Sort Key: `startTime#sessionId`  
**Attributes:**
- `therapistId` (string) - Therapist owner of session
- `sessionId` (string) - unique sessionId. 
- `startTime` (LocalDate) - start time
- `endTime` (LocalDate) - end time. 
- `status` (enum) - Available/booked/cancelled. 
- `privateNotes` (string) - Therapist only notes
- `sharedNotes` (string) - Client-therapist notes.

### Appointments Table
**Table Name:** `Appointments`  
**Primary Key:** 
- partition Key: `appointmentId`
**Attributes:**
- `appointmentId` (string) - unique Id
- `sessionId` (string) - Linked sessionId. 
- `clientId` (string) - Client Id
- `therapistId` (string) - therapist Id. 
- `status` (enum) - requested/confirmed/cancelled.
- `mappingId` (string) - created when status is confirmed. 
- `requestedAt` (LocalDateTime) - timestamp

### Mappings Table
**Table Name:** `Mappings`  
**Primary Key:**
- partition key: `mappingId`
**Global Secondary Index1:** `clientMappingGSI`
- partition key: `clientId`
- sort key: `therapistId` 
**Global Secondary Index2:** `TherapistMappingGSI`
- partition key: `therapistId`
- sort key: `clientId`
**Attributes:**
- `mappingId` (string) - Unique mappingId.
- `clientId` (string) - Client Id. 
- `therapistId` (string) - therapist Id. 
- `journalAccess` (boolean) - Access granted flag (true/false)
- `journalAccessRequested` (boolean) - Access granted flag. 
- `status` (enum) - pending/active/inactive.
- `lastUpdated` (LocalDateTime) - timestamp

### Messages Table
**Table Name:** `Messages`  
**Primary Key:** 
- partition key: `conversationId` (computed by taking senderId and receiverId) 
- sort Key: `timestamp#messageId`
**Global secondary index (GSI I):** `SenderMessageGSI`
- Partition Key: `senderId`
- sort key - `timestamp#messageId`
**Global secondary index (GSI II):** `receiverMessageGSI`
- Partition Key: `receiverId`
- sort key: `timestamp#messageId`
**Attributes:**
- `conversationId` (string) - Conversation Identifier
- `messageId` (string) - unique messageId. 
- `senderId` (string) - userId of sender. 
- `receiverId` (string) - userId of receiver.
- `content` (string) - Message text. 
- `timestamp` (LocalDateTime) - timestamp

## Implementation 

Key Components:

1. CDK Stack (TherapyJournalCdkStack.java)
   - Defines AWS resources (Lambda, API gateway and DynamoDB)
   - Implements Least-privilege IAM policies
   - Enables CLoudWatch logging
2. Lambda handlers
   - Seperate handlers for each API endpoint.
   - Input validation and error handling
   - DynamoDB data access layer.
3. Infrastructure as a code
   - Reproducable deployments
   - Environment specific configurations
  
## Deployment

### Prerequisites
  - AWS account with CLI configured
  - Java 17 JDK
  - Node js for CDK
  - Maven

### Steps
  1. Clone the repository
     - ```
       git clone https://github.com/Pragadeesh-19/therapy-journal-API.git
       ```
  2. Build project
     - ```
       mvn clean install
       ```
  3. Deploy infrastructure
     - ```
       cdk deploy
       ```

## Testing

  - [Postman Collection](./postman/therapy_journal.json)

## Assumptions
  1. Authenntication/Authorization handled at API gateway level
  2. Timestamps are in UTC format
  3. pagination are not implemented for this list of operations.
  4. All dates/times use ISO 8601 format
