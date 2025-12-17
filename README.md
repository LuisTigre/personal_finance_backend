# Personal Finance App

A full-stack personal finance application built with Java 17, Spring Boot, KeyCloak authentication, and Angular.

## Features

- 🔐 **Secure Authentication** with KeyCloak
- 💰 **Account Management** - Track multiple accounts (checking, savings, credit cards, etc.)
- 📊 **Transaction Tracking** - Record income, expenses, and transfers
- 📈 **Dashboard** - Visual overview of your financial status
- 🏷️ **Category Management** - Organize transactions by categories
- 💵 **Budget Tracking** - Set and monitor budgets
- 🌐 **RESTful API** - Clean API design with Spring Boot
- 🎨 **Modern UI** - Responsive Angular frontend

## Tech Stack

### Backend
- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** with OAuth2 Resource Server
- **Spring Data JPA**
- **KeyCloak 23.0.0** for authentication
- **PostgreSQL** for production database
- **H2** for development database
- **Maven** for build management

### Frontend
- **Angular 17**
- **TypeScript**
- **KeyCloak Angular Adapter**
- **RxJS**
- **Chart.js** for data visualization

### DevOps
- **Docker** & **Docker Compose**
- **Nginx** for frontend serving

## Architecture

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Angular   │─────▶│  Spring Boot │─────▶│  PostgreSQL │
│  Frontend   │      │   Backend    │      │  Database   │
└─────────────┘      └──────────────┘      └─────────────┘
       │                     │
       │                     │
       └─────────┬───────────┘
                 │
                 ▼
         ┌──────────────┐
         │   KeyCloak   │
         │ Auth Server  │
         └──────────────┘
```

## Prerequisites

- Java 17 or higher
- Node.js 18+ and npm
- Docker and Docker Compose (for full stack deployment)
- Maven 3.8+ (if running backend locally)

## Quick Start with Docker

The easiest way to run the entire application stack:

```bash
# Clone the repository
git clone https://github.com/LuisTigre/personal_finance_app.git
cd personal_finance_app

# Start all services with Docker Compose
docker-compose up -d

# Wait for services to start (about 30-60 seconds)
# KeyCloak will be available at: http://localhost:8080
# Backend API will be available at: http://localhost:8081
# Frontend will be available at: http://localhost:4200
```

### Default Credentials

- **KeyCloak Admin Console** (http://localhost:8080)
  - Username: `admin`
  - Password: `admin`

- **Test User** (for application login)
  - Username: `testuser`
  - Password: `test123`

## Local Development Setup

### Backend Setup

```bash
cd backend

# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/personal-finance-backend-1.0.0.jar
```

The backend will start on `http://localhost:8081`

### Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```

The frontend will start on `http://localhost:4200`

### KeyCloak Setup (Standalone)

```bash
# Using Docker
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:23.0.0 \
  start-dev
```

Then import the realm configuration:
1. Login to KeyCloak admin console at http://localhost:8080
2. Click on "Create Realm"
3. Import the file `keycloak/realm-export.json`

## API Documentation

### Authentication
All API endpoints (except `/api/public/**`) require a valid JWT token from KeyCloak.

Include the token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

### Endpoints

#### Users
- `GET /api/users/me` - Get current authenticated user

#### Accounts
- `GET /api/accounts` - List all accounts for current user
- `GET /api/accounts/{id}` - Get account by ID
- `POST /api/accounts` - Create new account
- `PUT /api/accounts/{id}` - Update account
- `DELETE /api/accounts/{id}` - Delete account

#### Transactions
- `GET /api/transactions` - List all transactions for current user
- `GET /api/transactions?startDate=2025-01-01&endDate=2025-12-31` - Filter by date range
- `GET /api/transactions/{id}` - Get transaction by ID
- `POST /api/transactions` - Create new transaction
- `PUT /api/transactions/{id}` - Update transaction
- `DELETE /api/transactions/{id}` - Delete transaction

## Database Schema

### Users Table
- id (PK)
- keycloak_id (unique)
- username
- email
- first_name
- last_name
- created_at
- updated_at

### Accounts Table
- id (PK)
- user_id (FK)
- name
- type (CHECKING, SAVINGS, CREDIT_CARD, INVESTMENT, CASH)
- balance
- currency
- description
- created_at
- updated_at

### Transactions Table
- id (PK)
- account_id (FK)
- category_id (FK)
- type (INCOME, EXPENSE, TRANSFER)
- amount
- transaction_date
- description
- payee
- notes
- created_at
- updated_at

### Categories Table
- id (PK)
- user_id (FK)
- name
- type (INCOME, EXPENSE)
- color
- icon
- description
- created_at
- updated_at

### Budgets Table
- id (PK)
- user_id (FK)
- category_id (FK)
- name
- amount
- start_date
- end_date
- period (WEEKLY, MONTHLY, QUARTERLY, YEARLY)
- created_at
- updated_at

## Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/financedb
    username: finance
    password: finance123
  
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/personal-finance

server:
  port: 8081
```

### Frontend Configuration

Edit `frontend/src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api',
  keycloak: {
    url: 'http://localhost:8080',
    realm: 'personal-finance',
    clientId: 'personal-finance-app'
  }
};
```

## Security

- **Authentication**: OAuth2/OpenID Connect via KeyCloak
- **Authorization**: Role-based access control (RBAC)
- **Password Storage**: Managed by KeyCloak with bcrypt hashing
- **Token Security**: JWT tokens with configurable expiration
- **CORS**: Configured for frontend origin
- **HTTPS**: Recommended for production deployment

## Testing

### Backend Tests
```bash
cd backend
mvn test
```

### Frontend Tests
```bash
cd frontend
npm test
```

## Building for Production

### Backend
```bash
cd backend
mvn clean package -DskipTests
# JAR file will be in target/personal-finance-backend-1.0.0.jar
```

### Frontend
```bash
cd frontend
npm run build
# Production files will be in dist/personal-finance-frontend/
```

## Deployment

### Using Docker Compose
```bash
docker-compose -f docker-compose.yml up -d
```

### Manual Deployment
1. Deploy PostgreSQL database
2. Deploy KeyCloak with realm configuration
3. Deploy Spring Boot backend (update application.yml with production values)
4. Build Angular frontend and deploy to web server (Nginx, Apache, etc.)

## Troubleshooting

### KeyCloak Connection Issues
- Ensure KeyCloak is running and accessible
- Check realm name and client ID match configuration
- Verify redirect URIs are correctly configured in KeyCloak

### Database Connection Issues
- Verify PostgreSQL is running
- Check database credentials
- Ensure database `financedb` exists

### CORS Issues
- Check allowed origins in backend SecurityConfig
- Verify frontend is running on allowed origin

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues and questions:
- Create an issue on GitHub
- Contact: support@personalfinance.example.com

## Roadmap

- [ ] Budget alerts and notifications
- [ ] Recurring transactions
- [ ] Financial reports and exports
- [ ] Multi-currency support
- [ ] Mobile application
- [ ] Bank account integration
- [ ] Investment tracking
- [ ] Bill payment reminders

---

Built with ❤️ using Java, Spring Boot, KeyCloak, and Angular