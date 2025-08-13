# 📅 EDBM - Gestion des Salles de Réunion

Application web complète de gestion et de réservation des salles de réunion pour l'Economic Development Board of Madagascar  (EDBM).

## 🎯 Fonctionnalités Principales

- **Réservation de salles** : Interface intuitive pour réserver des salles de réunion
- **Calendrier interactif** : Vue mensuelle/hebdomadaire des réservations
- **Gestion des récurrences** : Support des réservations récurrentes (quotidienne, hebdomadaire, mensuelle)
- **Notifications email** : Envoi automatique de rappels et confirmations
- **Tableau de bord** : Statistiques et rapports sur l'utilisation des salles
- **Authentification** : Système de connexion sécurisé avec rôles utilisateurs
- **Export PDF** : Génération de rapports imprimables

## 🛠️ Stack Technique

### Backend
- **Framework** : Spring Boot 3.x
- **Base de données** : PostgreSQL 15+
- **ORM** : Hibernate / JPA
- **Sécurité** : Spring Security avec JWT
- **Email** : Spring Mail avec SMTP


### Frontend
- **Framework** : Next.js 14 (React)
- **Styling** : CSS Modules
- **UI Components** : Composants personnalisés
- **Icons** : Font Awesome


## 📋 Prérequis

### Système
- **Java** : JDK 17 ou supérieur
- **Node.js** : 18.x ou supérieur
- **PostgreSQL** : 15.x ou supérieur
- **npm** : 9.x ou supérieur

### Configuration PostgreSQL
```sql
CREATE DATABASE edbm_meeting_room;
CREATE USER postgres WITH PASSWORD 'rova';
GRANT ALL PRIVILEGES ON DATABASE edbm_meeting_room TO postgres;
```

## 🚀 Installation

### 1. Cloner le projet
```bash
git clone [URL_DU_REPO]
cd edbm-meeting-room-management
```

### 2. Configuration Backend
```bash
cd edbm-meeting-room-management-backend/demo
```

#### Modifier application.properties
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/edbm_meeting_room
spring.datasource.username=postgres
spring.datasource.password=[VOTRE_MOT_DE_PASSE]
```

#### Lancer le backend
```bash
# Avec Maven
./mvnw spring-boot:run

# Ou avec le script
./start-backend.sh
```

### 3. Configuration Frontend
```bash
cd edbm-meeting-room-management-frontend
npm install
npm run dev
```

L'application sera accessible sur : http://localhost:3000

## 🔧 Configuration Email

Pour les notifications email, configurez les variables dans `application.properties` :

```properties
spring.mail.username=[VOTRE_EMAIL]
spring.mail.password=[VOTRE_MOT_DE_PASSE_APP]
```

## 📊 Structure de la Base de Données

### Tables Principales
- **reservations** : Réservations des salles
- **users** : Utilisateurs et authentification
- **email_logs** : Historique des emails envoyés
- **reservation_history** : Historique des modifications

## 🎮 Utilisation

### Pour les utilisateurs
1. Se connecter avec ses identifiants
2. Naviguer vers "Créer une réservation"
3. Sélectionner la salle, date et heure
4. Ajouter les participants et description
5. Valider la réservation

### Pour les administrateurs
- Accès au tableau de bord complet
- Gestion des utilisateurs
- Visualisation des statistiques d'utilisation
- Export des rapports

## 🧪 Tests

### Backend Tests
```bash
cd edbm-meeting-room-management-backend/demo
./mvnw test
```

### Frontend Tests
```bash
cd edbm-meeting-room-management-frontend
npm test
```

## 🐳 Docker (Optionnel)

### Lancer avec Docker Compose
```bash
docker-compose up --build
```

## 📁 Structure du Projet

```
edbm-meeting-room-management/
├── edbm-meeting-room-management-backend/
│   └── demo/
│       ├── src/main/java/edbm/salle/demo/
│       │   ├── controller/     # Contrôleurs REST
│       │   ├── service/        # Logique métier
│       │   ├── repository/     # Accès données
│       │   └── model/          # Entités JPA
│       └── src/main/resources/
│           └── application.properties
├── edbm-meeting-room-management-frontend/
│   ├── pages/                  # Pages Next.js
│   ├── components/            # Composants React
│   └── styles/                # Feuilles de style
├── edbm-meeting-room-management-infrastructure/
    ├── database/
    │   └── base_de_donnee.sql
    ├── deployment/
    │   ├── scripts/
    │   │   ├── start.sh, start.bat, fix-deployment.bat, etc.
    │   ├── docker/
    │   │   ├── docker-compose.yml, Dockerfile.frontend, Dockerfile.backend
    │   └── guides/
    │       ├── DEPLOYMENT_GUIDE.md, TROUBLESHOOTING.md, etc.
    └── README.md

```

## 🔍 API Endpoints Principaux

### Réservations
- `GET /api/reservations` - Liste des réservations
- `POST /api/reservations` - Créer une réservation
- `PUT /api/reservations/{id}` - Modifier une réservation
- `DELETE /api/reservations/{id}` - Annuler une réservation

### Statistiques
- `GET /api/statistics/weekly` - Statistiques hebdomadaires
- `GET /api/statistics/monthly` - Statistiques mensuelles

## 🚨 Dépannage

### Problèmes courants

**Erreur de connexion PostgreSQL**
```bash
# Vérifier que PostgreSQL est en cours
sudo systemctl start postgresql

# Vérifier les identifiants
psql -U postgres -d edbm_meeting_room
```

**Port déjà utilisé**
```bash
# Backend
server.port=8081 # dans application.properties

# Frontend
npm run dev -- -p 3001
```

## 🤝 Contribution

1. Fork le projet
2. Créer une branche (`feature/AmazingFeature`)
3. Commit vos changements (`git commit -m 'Add AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request




