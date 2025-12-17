# API Reference

## Base URL
```
http://localhost:8081/api
```

All endpoints require authentication except those under `/api/public/**`.

## Authentication

Include the JWT token in the Authorization header:
```
Authorization: Bearer <jwt-token>
```

Get your token from KeyCloak after logging in through the frontend.

---

## Users API

### Get Current User
Get the authenticated user's profile.

**Endpoint:** `GET /api/users/me`

**Response:**
```json
{
  "id": 1,
  "keycloakId": "uuid-here",
  "username": "testuser",
  "email": "test@example.com",
  "firstName": "Test",
  "lastName": "User",
  "createdAt": "2025-01-01T10:00:00",
  "updatedAt": "2025-01-01T10:00:00"
}
```

---

## Accounts API

### List All Accounts
Get all accounts for the authenticated user.

**Endpoint:** `GET /api/accounts`

**Response:**
```json
[
  {
    "id": 1,
    "name": "Main Checking",
    "type": "CHECKING",
    "balance": 5000.00,
    "currency": "USD",
    "description": "Primary checking account",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T10:00:00"
  }
]
```

### Get Account by ID
**Endpoint:** `GET /api/accounts/{id}`

### Create Account
**Endpoint:** `POST /api/accounts`

**Request Body:**
```json
{
  "name": "Savings Account",
  "type": "SAVINGS",
  "balance": 10000.00,
  "currency": "USD",
  "description": "Emergency fund"
}
```

**Account Types:**
- `CHECKING`
- `SAVINGS`
- `CREDIT_CARD`
- `INVESTMENT`
- `CASH`

### Update Account
**Endpoint:** `PUT /api/accounts/{id}`

**Request Body:** Same as Create Account

### Delete Account
**Endpoint:** `DELETE /api/accounts/{id}`

---

## Transactions API

### List All Transactions
Get all transactions for the authenticated user.

**Endpoint:** `GET /api/transactions`

**Query Parameters:**
- `startDate` (optional): Filter by start date (ISO format: YYYY-MM-DD)
- `endDate` (optional): Filter by end date (ISO format: YYYY-MM-DD)

**Example:**
```
GET /api/transactions?startDate=2025-01-01&endDate=2025-12-31
```

**Response:**
```json
[
  {
    "id": 1,
    "accountId": 1,
    "categoryId": 2,
    "type": "EXPENSE",
    "amount": 50.00,
    "transactionDate": "2025-01-15",
    "description": "Grocery shopping",
    "payee": "Supermarket",
    "notes": "Weekly groceries",
    "createdAt": "2025-01-15T10:00:00",
    "updatedAt": "2025-01-15T10:00:00"
  }
]
```

### Get Transaction by ID
**Endpoint:** `GET /api/transactions/{id}`

### Create Transaction
**Endpoint:** `POST /api/transactions`

**Request Body:**
```json
{
  "accountId": 1,
  "categoryId": 2,
  "type": "EXPENSE",
  "amount": 75.50,
  "transactionDate": "2025-01-20",
  "description": "Dinner",
  "payee": "Restaurant",
  "notes": "Team dinner"
}
```

**Transaction Types:**
- `INCOME`
- `EXPENSE`
- `TRANSFER`

### Update Transaction
**Endpoint:** `PUT /api/transactions/{id}`

**Request Body:** Same as Create Transaction

### Delete Transaction
**Endpoint:** `DELETE /api/transactions/{id}`

---

## Error Responses

### 401 Unauthorized
```json
{
  "timestamp": "2025-01-01T10:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required",
  "path": "/api/accounts"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-01-01T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Account not found with id: 123",
  "path": "/api/accounts/123"
}
```

### 400 Bad Request
```json
{
  "timestamp": "2025-01-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/accounts"
}
```

---

## Rate Limiting

Currently no rate limiting is implemented. For production deployment, consider implementing rate limiting to prevent abuse.

---

## Pagination

For future versions, pagination will be added to list endpoints:
```
GET /api/transactions?page=0&size=20&sort=transactionDate,desc
```

---

## Examples

### cURL Examples

**Get Current User:**
```bash
curl -X GET "http://localhost:8081/api/users/me" \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Create Account:**
```bash
curl -X POST "http://localhost:8081/api/accounts" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Savings",
    "type": "SAVINGS",
    "balance": 5000.00,
    "currency": "USD"
  }'
```

**Create Transaction:**
```bash
curl -X POST "http://localhost:8081/api/transactions" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 1,
    "type": "EXPENSE",
    "amount": 50.00,
    "transactionDate": "2025-01-20",
    "description": "Groceries"
  }'
```

### JavaScript/TypeScript Examples

**Using Fetch API:**
```javascript
// Get accounts
const response = await fetch('http://localhost:8081/api/accounts', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const accounts = await response.json();

// Create transaction
const response = await fetch('http://localhost:8081/api/transactions', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    accountId: 1,
    type: 'EXPENSE',
    amount: 50.00,
    transactionDate: '2025-01-20',
    description: 'Groceries'
  })
});
const transaction = await response.json();
```
