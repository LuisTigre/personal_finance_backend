# Development Guide

## Setting Up Your Development Environment

This guide will help you set up the Personal Finance App for local development.

## Prerequisites

- **Java 17 JDK** ([Download](https://adoptium.net/))
- **Maven 3.8+** ([Download](https://maven.apache.org/download.cgi))
- **Node.js 18+** and npm ([Download](https://nodejs.org/))
- **Docker Desktop** ([Download](https://www.docker.com/products/docker-desktop))
- **Git** ([Download](https://git-scm.com/downloads))
- **IDE**: IntelliJ IDEA, VS Code, or Eclipse

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/LuisTigre/personal_finance_app.git
cd personal_finance_app
```

### 2. Start KeyCloak and PostgreSQL

```bash
docker-compose up -d postgres keycloak
```

Wait about 30 seconds for services to start, then verify:
```bash
docker-compose ps
```

### 3. Run the Backend

```bash
cd backend
mvn spring-boot:run
```

Backend will be available at http://localhost:8081

### 4. Run the Frontend

In a new terminal:
```bash
cd frontend
npm install
npm start
```

Frontend will be available at http://localhost:4200

## Development Workflow

### Backend Development

#### Project Structure
```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/personalfinance/app/
│   │   │   ├── config/         # Security and app configuration
│   │   │   ├── controller/     # REST API endpoints
│   │   │   ├── model/          # JPA entities
│   │   │   ├── repository/     # Data access layer
│   │   │   ├── service/        # Business logic
│   │   │   └── PersonalFinanceApplication.java
│   │   └── resources/
│   │       └── application.yml # Application configuration
│   └── test/
└── pom.xml
```

#### Running Tests
```bash
mvn test
```

#### Building
```bash
mvn clean package
```

#### Hot Reload
Use Spring Boot DevTools (already included in pom.xml). Changes to Java files will automatically restart the application.

#### Debugging
In IntelliJ IDEA:
1. Right-click on `PersonalFinanceApplication.java`
2. Select "Debug 'PersonalFinanceApplication'"

In VS Code:
1. Install "Spring Boot Extension Pack"
2. Press F5 to start debugging

### Frontend Development

#### Project Structure
```
frontend/
├── src/
│   ├── app/
│   │   ├── components/     # UI components
│   │   ├── services/       # API services
│   │   ├── guards/         # Route guards
│   │   ├── models/         # TypeScript interfaces
│   │   ├── app.component.ts
│   │   └── app.config.ts
│   ├── environments/       # Environment configs
│   ├── assets/            # Static files
│   ├── index.html
│   ├── main.ts
│   └── styles.css
├── angular.json
├── package.json
└── tsconfig.json
```

#### Development Server
```bash
npm start
# or
ng serve
```

Access at http://localhost:4200. The app will automatically reload on file changes.

#### Building for Production
```bash
npm run build
```

Output will be in `dist/personal-finance-frontend/`

#### Running Tests
```bash
npm test
```

#### Linting
```bash
npm run lint
```

## Creating New Features

### Adding a New Entity

1. **Create Model** (`backend/src/main/java/com/personalfinance/app/model/`)
```java
@Entity
@Table(name = "goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Goal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    private String name;
    private BigDecimal targetAmount;
    private LocalDate targetDate;
}
```

2. **Create Repository** (`backend/src/main/java/com/personalfinance/app/repository/`)
```java
@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserId(Long userId);
}
```

3. **Create Service** (`backend/src/main/java/com/personalfinance/app/service/`)
```java
@Service
@Transactional
public class GoalService {
    private final GoalRepository goalRepository;
    
    public GoalService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }
    
    public List<Goal> getUserGoals(Long userId) {
        return goalRepository.findByUserId(userId);
    }
}
```

4. **Create Controller** (`backend/src/main/java/com/personalfinance/app/controller/`)
```java
@RestController
@RequestMapping("/api/goals")
public class GoalController {
    private final GoalService goalService;
    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<List<Goal>> getGoals(Authentication auth) {
        User user = userService.getCurrentUser(auth);
        return ResponseEntity.ok(goalService.getUserGoals(user.getId()));
    }
}
```

5. **Add Frontend Model** (`frontend/src/app/models/models.ts`)
```typescript
export interface Goal {
  id?: number;
  name: string;
  targetAmount: number;
  targetDate: string;
}
```

6. **Create Frontend Service** (`frontend/src/app/services/goal.service.ts`)
```typescript
@Injectable({ providedIn: 'root' })
export class GoalService {
  private apiUrl = `${environment.apiUrl}/goals`;
  
  constructor(private http: HttpClient) {}
  
  getGoals(): Observable<Goal[]> {
    return this.http.get<Goal[]>(this.apiUrl);
  }
}
```

## Database Management

### H2 Console (Development)
Access at http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:financedb`
- Username: `sa`
- Password: (leave empty)

### PostgreSQL (Production)
```bash
# Connect to database
docker exec -it personal-finance-postgres psql -U finance -d financedb

# List tables
\dt

# Describe table
\d users

# Run query
SELECT * FROM users;
```

### Database Migrations
For production, consider using Flyway or Liquibase. Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

Create migration in `src/main/resources/db/migration/V1__initial_schema.sql`

## KeyCloak Configuration

### Access KeyCloak Admin Console
1. Go to http://localhost:8080
2. Login with `admin` / `admin`
3. Select "personal-finance" realm

### Creating a Test User
1. Go to "Users" → "Add user"
2. Enter username and email
3. Save
4. Go to "Credentials" tab
5. Set password (uncheck "Temporary")

### Configuring Client
1. Go to "Clients" → "personal-finance-app"
2. Update "Valid Redirect URIs" if needed
3. Update "Web Origins" to allow CORS

## API Testing

### Using cURL
```bash
# Get token
TOKEN=$(curl -X POST http://localhost:8080/realms/personal-finance/protocol/openid-connect/token \
  -d "client_id=personal-finance-app" \
  -d "username=testuser" \
  -d "password=test123" \
  -d "grant_type=password" | jq -r '.access_token')

# Use token
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/users/me
```

### Using Postman
1. Import the API collection (create one if needed)
2. Set up environment variables
3. Configure OAuth2 authentication

### Using Thunder Client (VS Code)
1. Install Thunder Client extension
2. Create new request
3. Set Auth to OAuth2
4. Configure with KeyCloak settings

## Debugging Tips

### Backend Issues

**Port already in use:**
```bash
# Find process
lsof -i :8081

# Kill process
kill -9 <PID>
```

**Database connection error:**
- Check PostgreSQL is running: `docker ps`
- Verify credentials in `application.yml`

**KeyCloak authentication error:**
- Check KeyCloak is running
- Verify realm and client configuration
- Check token expiration

### Frontend Issues

**Cannot connect to backend:**
- Verify backend is running on port 8081
- Check CORS configuration
- Verify API URL in environment.ts

**KeyCloak redirect issues:**
- Check redirect URIs in KeyCloak client config
- Verify KeyCloak URL in environment.ts

**Build errors:**
```bash
# Clear cache
rm -rf node_modules package-lock.json
npm install
```

## Code Style

### Backend (Java)
- Follow Google Java Style Guide
- Use Lombok for boilerplate code
- Add JavaDoc for public methods
- Keep methods under 20 lines

### Frontend (TypeScript)
- Follow Angular Style Guide
- Use TypeScript strict mode
- Add JSDoc comments
- Use async/await instead of promises

## Git Workflow

### Branching Strategy
- `main` - production-ready code
- `develop` - development branch
- `feature/feature-name` - feature branches
- `bugfix/bug-description` - bug fix branches

### Commit Messages
Follow Conventional Commits:
```
feat: add goal tracking feature
fix: resolve transaction date validation issue
docs: update API documentation
style: format code with prettier
refactor: simplify account service logic
test: add tests for transaction service
```

### Pull Request Process
1. Create feature branch from `develop`
2. Make changes and commit
3. Push branch and create PR
4. Request review
5. Address feedback
6. Merge after approval

## Performance Optimization

### Backend
- Use pagination for large datasets
- Add database indexes
- Cache frequently accessed data
- Use @Transactional wisely

### Frontend
- Use OnPush change detection
- Lazy load routes
- Optimize images
- Use virtual scrolling for large lists

## Security Best Practices

- Never commit secrets or credentials
- Use environment variables
- Validate all user inputs
- Use parameterized queries
- Keep dependencies updated
- Enable HTTPS in production

## Common Tasks

### Add New Dependency

Backend:
```xml
<dependency>
    <groupId>group.id</groupId>
    <artifactId>artifact-id</artifactId>
    <version>1.0.0</version>
</dependency>
```

Frontend:
```bash
npm install package-name
```

### Create Database Backup
```bash
docker exec personal-finance-postgres pg_dump -U finance financedb > backup.sql
```

### Restore Database
```bash
docker exec -i personal-finance-postgres psql -U finance financedb < backup.sql
```

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Angular Documentation](https://angular.io/docs)
- [KeyCloak Documentation](https://www.keycloak.org/documentation)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## Getting Help

- Create an issue on GitHub
- Check existing issues for similar problems
- Review API documentation
- Check application logs

## Next Steps

1. Explore the codebase
2. Run the application
3. Make a small change
4. Write tests
5. Create a feature branch
6. Submit a pull request

Happy coding! 🚀
