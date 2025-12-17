# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2025-12-17

### Security
- **CRITICAL**: Updated Angular from 17.x to 19.2.17 to fix multiple security vulnerabilities:
  - Fixed XSRF Token Leakage via Protocol-Relative URLs in Angular HTTP Client
  - Fixed Stored XSS Vulnerability via SVG Animation, SVG URL and MathML Attributes
- Updated KeyCloak Angular adapter from 15.2.1 to 16.0.1 for Angular 19 compatibility
- Updated zone.js from 0.14.2 to 0.15.0
- Updated TypeScript from 5.2.2 to 5.4.5
- Updated Angular CLI and build tools to 19.2.16

### Changed
- Updated all Angular dependencies to version 19.2.17 (patched version)
- Updated TypeScript module resolution from "node" to "bundler"
- Updated documentation to reflect Angular 19 usage

### Affected Components
- Frontend package.json
- Frontend TypeScript configuration
- Documentation files (README.md, SECURITY.md, IMPLEMENTATION.md)

## [1.0.0] - 2025-12-17

### Added
- Initial release of Personal Finance Application
- Java 17 + Spring Boot 3.2.0 backend
- Angular 17 frontend (later updated to 19.2.17)
- KeyCloak 23.0.0 authentication
- RESTful API for financial data management
- Docker Compose setup for full stack
- Comprehensive documentation (README, API, DEVELOPMENT, DEPLOYMENT, SECURITY)

### Features
- User authentication via KeyCloak OAuth2/OpenID Connect
- Account management (multiple account types)
- Transaction tracking (income, expenses, transfers)
- Category management
- Budget tracking
- Dashboard with financial overview
- Responsive UI design
- JWT-based stateless authentication

### Security
- OAuth2/OpenID Connect authentication
- JWT token validation
- Role-based access control
- CORS protection
- SQL injection prevention via JPA
- Secure password management via KeyCloak

### Infrastructure
- Docker containerization
- PostgreSQL for production
- H2 for development
- Nginx reverse proxy
- Automated startup scripts

### Documentation
- README.md - Project overview
- API.md - API reference
- DEVELOPMENT.md - Development guide
- DEPLOYMENT.md - Production deployment
- IMPLEMENTATION.md - Implementation details
- SECURITY.md - Security documentation

---

## Version History

- **v1.0.1** - Security patch for Angular vulnerabilities
- **v1.0.0** - Initial release
