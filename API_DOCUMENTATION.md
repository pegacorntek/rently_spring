# Rently API Documentation

**Base URL:** `/api/v1`

All responses are wrapped in `ApiResponse`:
```json
{
  "success": true,
  "data": {...},
  "message": "Optional message"
}
```

---

## Authentication

### POST `/auth/register/request-otp`
Request OTP for registration.

**Request:**
```json
{
  "phone": "0901234567",
  "type": "REGISTRATION"
}
```

**Response:** `ApiResponse<null>`

---

### POST `/auth/register/verify-otp`
Verify OTP.

**Request:**
```json
{
  "phone": "0901234567",
  "otp": "123456"
}
```

**Response:** `ApiResponse<null>`

---

### POST `/auth/register/complete`
Complete registration.

**Request:**
```json
{
  "phone": "0901234567",
  "otp": "123456",
  "password": "password123",
  "retypePassword": "password123",
  "fullName": "John Doe"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "uuid",
      "phone": "0901234567",
      "fullName": "John Doe",
      "email": null,
      "roles": ["LANDLORD"],
      "status": "ACTIVE",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    },
    "token": "jwt_token_here"
  }
}
```

---

### POST `/auth/login`
Login with phone and password.

**Request:**
```json
{
  "phone": "0901234567",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "uuid",
      "phone": "0901234567",
      "fullName": "John Doe",
      "email": null,
      "roles": ["LANDLORD"],
      "status": "ACTIVE",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    },
    "token": "jwt_token_here"
  }
}
```

---

### POST `/auth/forgot/request-otp`
Request OTP for password reset.

**Request:**
```json
{
  "phone": "0901234567"
}
```

**Response:** `ApiResponse<null>`

---

### POST `/auth/forgot/reset-password`
Reset password with OTP.

**Request:**
```json
{
  "phone": "0901234567",
  "otp": "123456",
  "newPassword": "newpassword123",
  "retypePassword": "newpassword123"
}
```

**Response:** `ApiResponse<null>`

---

## Current User

### GET `/me`
Get current user profile. *Requires Authentication*

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "phone": "0901234567",
    "fullName": "John Doe",
    "email": "john@example.com",
    "roles": ["LANDLORD"],
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

---

## Landlord - Houses

*Requires Role: LANDLORD*

### GET `/landlord/houses`
Get all houses owned by landlord.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "ownerId": "uuid",
      "name": "House A",
      "address": "123 Street",
      "description": "Nice house",
      "status": "ACTIVE",
      "roomCount": 10,
      "tenantCount": 8,
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

### GET `/landlord/houses/{id}`
Get house by ID.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "ownerId": "uuid",
    "name": "House A",
    "address": "123 Street",
    "description": "Nice house",
    "status": "ACTIVE",
    "roomCount": 10,
    "tenantCount": 8,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

---

### POST `/landlord/houses`
Create a new house.

**Request:**
```json
{
  "name": "House A",
  "address": "123 Street",
  "description": "Nice house"
}
```

**Response:** `ApiResponse<HouseDto>`

---

### PUT `/landlord/houses/{id}`
Update a house.

**Request:**
```json
{
  "name": "Updated House A",
  "address": "456 Street",
  "description": "Updated description",
  "status": "INACTIVE"
}
```
*Status enum: ACTIVE, INACTIVE*

**Response:** `ApiResponse<HouseDto>`

---

### DELETE `/landlord/houses/{id}`
Delete a house.

**Response:** `ApiResponse<null>`

---

## Landlord - Rooms

*Requires Role: LANDLORD*

### GET `/landlord/houses/{houseId}/rooms`
Get all rooms in a house.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "houseId": "uuid",
      "code": "A101",
      "floor": 1,
      "areaM2": 25.5,
      "baseRent": 3000000,
      "status": "OCCUPIED",
      "tenants": [
        {
          "id": "uuid",
          "userId": "uuid",
          "fullName": "Jane Doe",
          "phone": "0987654321",
          "isPrimary": true,
          "joinedAt": "2024-01-01T00:00:00"
        }
      ],
      "currentContract": {
        "id": "uuid",
        "startDate": "2024-01-01",
        "endDate": "2025-01-01",
        "monthlyRent": 3500000,
        "deposit": 7000000
      },
      "debt": 0,
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

### GET `/landlord/rooms/{id}`
Get room by ID.

**Response:** `ApiResponse<RoomDto>`

---

### POST `/landlord/rooms`
Create a new room.

**Request:**
```json
{
  "houseId": "uuid",
  "code": "A101",
  "floor": 1,
  "areaM2": 25.5,
  "baseRent": 3000000
}
```

**Response:** `ApiResponse<RoomDto>`

---

### PUT `/landlord/rooms/{id}`
Update a room.

**Request:**
```json
{
  "code": "A102",
  "floor": 2,
  "areaM2": 30.0,
  "baseRent": 3500000,
  "status": "AVAILABLE"
}
```
*Status enum: AVAILABLE, OCCUPIED, MAINTENANCE*

**Response:** `ApiResponse<RoomDto>`

---

### DELETE `/landlord/rooms/{id}`
Delete a room.

**Response:** `ApiResponse<null>`

---

### POST `/landlord/rooms/{roomId}/tenants`
Add tenant to room.

**Request:**
```json
{
  "phone": "0987654321"
}
```

**Response:** `ApiResponse<null>`

---

### DELETE `/landlord/rooms/{roomId}/tenants/{tenantId}`
Remove tenant from room.

**Response:** `ApiResponse<null>`

---

## Landlord - Contracts

*Requires Role: LANDLORD*

### GET `/landlord/contracts`
Get all contracts.

**Query Parameters:**
- `houseId` (optional): Filter by house
- `status` (optional): Filter by status (DRAFT, ACTIVE, ENDED, CANCELLED)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "roomId": "uuid",
      "room": {
        "id": "uuid",
        "code": "A101",
        "houseName": "House A"
      },
      "landlordId": "uuid",
      "tenantId": "uuid",
      "tenant": {
        "id": "uuid",
        "fullName": "Jane Doe",
        "phone": "0987654321"
      },
      "startDate": "2024-01-01",
      "endDate": "2025-01-01",
      "monthlyRent": 3500000,
      "depositAmount": 7000000,
      "depositPaid": true,
      "status": "ACTIVE",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

### GET `/landlord/contracts/{id}`
Get contract by ID.

**Response:** `ApiResponse<ContractDto>`

---

### POST `/landlord/contracts`
Create a new contract.

**Request:**
```json
{
  "roomId": "uuid",
  "tenantPhone": "0987654321",
  "startDate": "2024-01-01",
  "endDate": "2025-01-01",
  "monthlyRent": 3500000,
  "depositAmount": 7000000
}
```

**Response:** `ApiResponse<ContractDto>`

---

### PUT `/landlord/contracts/{id}`
Update a contract.

**Request:**
```json
{
  "endDate": "2025-06-01",
  "monthlyRent": 4000000
}
```

**Response:** `ApiResponse<ContractDto>`

---

### PUT `/landlord/contracts/{id}/activate`
Activate a draft contract.

**Response:** `ApiResponse<ContractDto>`

---

### PUT `/landlord/contracts/{id}/end`
End an active contract.

**Response:** `ApiResponse<ContractDto>`

---

## Tenant - Contracts

*Requires Role: TENANT*

### GET `/tenant/contracts`
Get my contracts.

**Response:** `ApiResponse<List<ContractDto>>`

---

## Landlord - Invoices

*Requires Role: LANDLORD*

### GET `/landlord/invoices`
Get all invoices.

**Query Parameters:**
- `houseId` (optional): Filter by house
- `status` (optional): Filter by status (DRAFT, SENT, PAID, OVERDUE, CANCELLED)
- `month` (optional): Filter by period month (YYYY-MM)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "contractId": "uuid",
      "tenantId": "uuid",
      "tenant": {
        "fullName": "Jane Doe",
        "phone": "0987654321"
      },
      "room": {
        "code": "A101",
        "houseName": "House A"
      },
      "periodMonth": "2024-01",
      "dueDate": "2024-01-10",
      "items": [
        {
          "id": "uuid",
          "type": "RENT",
          "description": "Monthly Rent",
          "quantity": 1,
          "unitPrice": 3500000,
          "amount": 3500000
        },
        {
          "id": "uuid",
          "type": "ELECTRICITY",
          "description": "Electricity (100 kWh)",
          "quantity": 100,
          "unitPrice": 3500,
          "amount": 350000
        }
      ],
      "totalAmount": 3850000,
      "paidAmount": 0,
      "lateFeePercent": 5,
      "status": "SENT",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

### GET `/landlord/invoices/{id}`
Get invoice by ID.

**Response:** `ApiResponse<InvoiceDto>`

---

### POST `/landlord/invoices/generate`
Generate a new invoice.

**Request:**
```json
{
  "contractId": "uuid",
  "periodMonth": "2024-01",
  "dueDate": "2024-01-10",
  "items": [
    {
      "type": "RENT",
      "description": "Monthly Rent",
      "quantity": 1,
      "unitPrice": 3500000,
      "amount": 3500000
    },
    {
      "type": "ELECTRICITY",
      "description": "Electricity (100 kWh)",
      "quantity": 100,
      "unitPrice": 3500,
      "amount": 350000
    }
  ],
  "lateFeePercent": 5
}
```
*Item types: RENT, ELECTRICITY, WATER, INTERNET, PARKING, SERVICE, OTHER*

**Response:** `ApiResponse<InvoiceDto>`

---

### PUT `/landlord/invoices/{id}/send`
Send invoice to tenant.

**Response:** `ApiResponse<InvoiceDto>`

---

### PUT `/landlord/invoices/{id}/cancel`
Cancel an invoice.

**Response:** `ApiResponse<InvoiceDto>`

---

### POST `/landlord/meter-readings`
Save meter reading.

**Request:**
```json
{
  "roomId": "uuid",
  "periodMonth": "2024-01",
  "electricityOld": 1000,
  "electricityNew": 1100,
  "electricityUnitPrice": 3500,
  "waterOld": 50,
  "waterNew": 60,
  "waterUnitPrice": 15000
}
```

**Response:** `ApiResponse<null>`

---

### GET `/landlord/rooms/{roomId}/meter-readings`
Get meter readings for a room.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "roomId": "uuid",
      "periodMonth": "2024-01",
      "electricityOld": 1000,
      "electricityNew": 1100,
      "electricityUnitPrice": 3500,
      "waterOld": 50,
      "waterNew": 60,
      "waterUnitPrice": 15000
    }
  ]
}
```

---

## Tenant - Invoices

*Requires Role: TENANT*

### GET `/tenant/invoices`
Get my invoices.

**Response:** `ApiResponse<List<InvoiceDto>>`

---

### GET `/tenant/invoices/{id}`
Get my invoice by ID.

**Response:** `ApiResponse<InvoiceDto>`

---

## Landlord - Payments

*Requires Role: LANDLORD*

### GET `/landlord/invoices/{invoiceId}/payments`
Get payments for an invoice.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "invoiceId": "uuid",
      "amount": 3850000,
      "method": "CASH",
      "status": "CONFIRMED",
      "transactionCode": null,
      "proofImageUrl": null,
      "paidAt": "2024-01-05T10:00:00",
      "createdAt": "2024-01-05T10:00:00"
    }
  ]
}
```

---

### POST `/landlord/payments/cash`
Record a cash payment.

**Request:**
```json
{
  "invoiceId": "uuid",
  "amount": 3850000,
  "note": "Paid in full"
}
```

**Response:** `ApiResponse<PaymentDto>`

---

### POST `/landlord/payments/{paymentId}/confirm`
Confirm a QR payment.

**Response:** `ApiResponse<PaymentDto>`

---

## Tenant - Payments

*Requires Role: TENANT*

### GET `/tenant/payments`
Get my payments.

**Response:** `ApiResponse<List<PaymentDto>>`

---

### POST `/tenant/payments/init`
Initialize a payment.

**Request:**
```json
{
  "invoiceId": "uuid",
  "method": "BANK_TRANSFER",
  "amount": 3850000
}
```
*Methods: CASH, BANK_TRANSFER, MOMO, ZALOPAY, VNPAY*

**Response:**
```json
{
  "success": true,
  "data": {
    "paymentId": "uuid",
    "paymentUrl": "https://payment.example.com/...",
    "qrCode": "data:image/png;base64,..."
  }
}
```

---

### POST `/tenant/payments/{paymentId}/upload-proof`
Upload payment proof (multipart/form-data).

**Form Data:**
- `proof`: File (image)

**Response:** `ApiResponse<PaymentDto>`

---

## Landlord - Tickets

*Requires Role: LANDLORD*

### GET `/landlord/tickets`
Get all tickets.

**Query Parameters:**
- `houseId` (optional): Filter by house
- `status` (optional): Filter by status (OPEN, IN_PROGRESS, RESOLVED, CLOSED)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "houseId": "uuid",
      "roomId": "uuid",
      "room": {
        "code": "A101"
      },
      "tenantId": "uuid",
      "tenant": {
        "fullName": "Jane Doe",
        "phone": "0987654321"
      },
      "title": "Broken AC",
      "description": "The air conditioner is not working",
      "attachments": [
        {
          "id": "uuid",
          "type": "IMAGE",
          "fileUrl": "/uploads/ticket_abc.jpg"
        }
      ],
      "status": "OPEN",
      "createdAt": "2024-01-15T10:00:00",
      "updatedAt": "2024-01-15T10:00:00"
    }
  ]
}
```

---

### GET `/landlord/tickets/{id}`
Get ticket by ID.

**Response:** `ApiResponse<TicketDto>`

---

### PUT `/landlord/tickets/{id}`
Update ticket status.

**Request:**
```json
{
  "status": "IN_PROGRESS"
}
```
*Status enum: OPEN, IN_PROGRESS, RESOLVED, CLOSED*

**Response:** `ApiResponse<TicketDto>`

---

## Tenant - Tickets

*Requires Role: TENANT*

### GET `/tenant/tickets`
Get my tickets.

**Response:** `ApiResponse<List<TicketDto>>`

---

### POST `/tenant/tickets`
Create a new ticket (multipart/form-data).

**Form Data:**
- `roomId`: String (required)
- `title`: String (required)
- `description`: String (required)
- `attachments`: File[] (optional)

**Response:** `ApiResponse<TicketDto>`

---

## Enums Reference

### User Status
- `ACTIVE`
- `INACTIVE`
- `SUSPENDED`

### User Roles
- `LANDLORD`
- `TENANT`
- `SYSTEM_ADMIN`

### House Status
- `ACTIVE`
- `INACTIVE`

### Room Status
- `AVAILABLE`
- `OCCUPIED`
- `MAINTENANCE`

### Contract Status
- `DRAFT`
- `ACTIVE`
- `ENDED`
- `CANCELLED`

### Invoice Status
- `DRAFT`
- `SENT`
- `PAID`
- `OVERDUE`
- `CANCELLED`

### Invoice Item Types
- `RENT`
- `ELECTRICITY`
- `WATER`
- `INTERNET`
- `PARKING`
- `SERVICE`
- `OTHER`

### Payment Methods
- `CASH`
- `BANK_TRANSFER`
- `MOMO`
- `ZALOPAY`
- `VNPAY`

### Payment Status
- `PENDING`
- `CONFIRMED`
- `REJECTED`

### Ticket Status
- `OPEN`
- `IN_PROGRESS`
- `RESOLVED`
- `CLOSED`

### Attachment Types
- `IMAGE`
- `VIDEO`
- `DOCUMENT`

### OTP Types
- `REGISTRATION`
- `PASSWORD_RESET`
