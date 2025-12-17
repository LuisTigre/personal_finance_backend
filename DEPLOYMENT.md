# Deployment Guide

## Production Deployment

This guide covers deploying the Personal Finance App to production.

## Prerequisites

- Linux server (Ubuntu 20.04+ recommended)
- Docker and Docker Compose installed
- Domain name (optional, for HTTPS)
- SSL certificate (Let's Encrypt recommended)

## 1. Server Preparation

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Install Docker Compose
sudo apt install docker-compose -y

# Create application directory
mkdir -p /opt/personal-finance-app
cd /opt/personal-finance-app
```

## 2. Clone Repository

```bash
git clone https://github.com/LuisTigre/personal_finance_app.git .
```

## 3. Configure Environment Variables

Create a `.env` file for production:

```bash
cat > .env << 'EOF'
# Database
POSTGRES_DB=financedb
POSTGRES_USER=finance
POSTGRES_PASSWORD=your-strong-password-here

# KeyCloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=your-admin-password-here

# Application
APP_DOMAIN=yourdomain.com
KEYCLOAK_URL=https://auth.yourdomain.com
BACKEND_URL=https://api.yourdomain.com
FRONTEND_URL=https://yourdomain.com
EOF
```

## 4. Production Docker Compose

Create `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: finance-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - finance-network
    restart: unless-stopped

  keycloak:
    image: quay.io/keycloak/keycloak:23.0.0
    container_name: finance-keycloak
    command: start --import-realm
    environment:
      KEYCLOAK_ADMIN: ${KEYCLOAK_ADMIN}
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      KC_DB_USERNAME: ${POSTGRES_USER}
      KC_DB_PASSWORD: ${POSTGRES_PASSWORD}
      KC_HOSTNAME: ${KEYCLOAK_URL}
      KC_HOSTNAME_STRICT: true
      KC_PROXY: edge
      KC_HTTP_ENABLED: false
      KC_HTTPS_ENABLED: true
    volumes:
      - ./keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json
    depends_on:
      - postgres
    networks:
      - finance-network
    restart: unless-stopped
    ports:
      - "8080:8080"

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: finance-backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: ${KEYCLOAK_URL}/realms/personal-finance
    depends_on:
      - postgres
      - keycloak
    networks:
      - finance-network
    restart: unless-stopped

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: finance-frontend
    depends_on:
      - backend
    networks:
      - finance-network
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    container_name: finance-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/ssl:/etc/nginx/ssl
    depends_on:
      - frontend
      - backend
      - keycloak
    networks:
      - finance-network
    restart: unless-stopped

volumes:
  postgres_data:

networks:
  finance-network:
    driver: bridge
```

## 5. Nginx Configuration

Create `nginx/nginx.conf`:

```nginx
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server backend:8081;
    }

    upstream frontend {
        server frontend:80;
    }

    upstream keycloak {
        server keycloak:8080;
    }

    # Redirect HTTP to HTTPS
    server {
        listen 80;
        server_name yourdomain.com api.yourdomain.com auth.yourdomain.com;
        return 301 https://$server_name$request_uri;
    }

    # Frontend
    server {
        listen 443 ssl http2;
        server_name yourdomain.com;

        ssl_certificate /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;

        location / {
            proxy_pass http://frontend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }

    # Backend API
    server {
        listen 443 ssl http2;
        server_name api.yourdomain.com;

        ssl_certificate /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;

        location / {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }

    # KeyCloak
    server {
        listen 443 ssl http2;
        server_name auth.yourdomain.com;

        ssl_certificate /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;

        location / {
            proxy_pass http://keycloak;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
```

## 6. SSL Certificate (Let's Encrypt)

```bash
# Install certbot
sudo apt install certbot -y

# Get certificate
sudo certbot certonly --standalone -d yourdomain.com -d api.yourdomain.com -d auth.yourdomain.com

# Copy certificates
sudo mkdir -p nginx/ssl
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem nginx/ssl/
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem nginx/ssl/
```

## 7. Deploy Application

```bash
# Build and start services
docker-compose -f docker-compose.prod.yml up -d

# Check logs
docker-compose -f docker-compose.prod.yml logs -f

# Check status
docker-compose -f docker-compose.prod.yml ps
```

## 8. Database Backup

Create a backup script:

```bash
cat > backup.sh << 'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/opt/backups"
mkdir -p $BACKUP_DIR

docker exec finance-postgres pg_dump -U finance financedb > $BACKUP_DIR/backup_$DATE.sql

# Keep only last 7 days of backups
find $BACKUP_DIR -name "backup_*.sql" -mtime +7 -delete
EOF

chmod +x backup.sh

# Add to crontab (daily at 2 AM)
echo "0 2 * * * /opt/personal-finance-app/backup.sh" | crontab -
```

## 9. Monitoring

```bash
# View logs
docker-compose -f docker-compose.prod.yml logs -f backend
docker-compose -f docker-compose.prod.yml logs -f frontend
docker-compose -f docker-compose.prod.yml logs -f keycloak

# Monitor resources
docker stats
```

## 10. Updating the Application

```bash
# Pull latest changes
git pull origin main

# Rebuild and restart
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml build
docker-compose -f docker-compose.prod.yml up -d
```

## Security Checklist

- [ ] Change all default passwords
- [ ] Enable HTTPS with valid SSL certificates
- [ ] Configure firewall (UFW)
- [ ] Set up fail2ban for brute force protection
- [ ] Enable database backups
- [ ] Configure log rotation
- [ ] Set up monitoring and alerts
- [ ] Use environment variables for secrets
- [ ] Restrict database access
- [ ] Keep Docker images updated

## Firewall Configuration

```bash
# Install UFW
sudo apt install ufw -y

# Configure rules
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Enable firewall
sudo ufw enable
```

## Troubleshooting

### Service won't start
```bash
# Check logs
docker-compose -f docker-compose.prod.yml logs service-name

# Check if ports are in use
sudo netstat -tulpn | grep LISTEN
```

### Database connection issues
```bash
# Check database is running
docker exec finance-postgres pg_isready

# Test connection
docker exec -it finance-postgres psql -U finance -d financedb
```

### SSL issues
```bash
# Test certificate
openssl s_client -connect yourdomain.com:443

# Renew certificate
sudo certbot renew
```

## Performance Tuning

### PostgreSQL
```sql
-- In postgres container
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
```

### JVM Tuning
Update backend service in docker-compose:
```yaml
backend:
  environment:
    JAVA_OPTS: "-Xmx512m -Xms256m"
```

## Maintenance

### Regular Tasks
- Weekly: Review logs for errors
- Weekly: Check disk space
- Monthly: Update Docker images
- Monthly: Review security advisories
- Quarterly: Review and update dependencies

### Health Checks
```bash
# Backend health
curl https://api.yourdomain.com/actuator/health

# Frontend
curl -I https://yourdomain.com

# KeyCloak
curl https://auth.yourdomain.com/health
```
