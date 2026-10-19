# JEE_Project_K
Le projet de JEE du groupe K : plateforme web de gestion d'assurance (clients, contrats, garanties, sinistres, paiements, assistant IA, statistiques).

Sujet : [doc/projet2026_2027.pdf](doc/projet2026_2027.pdf)

## Organisation du dépôt

```
doc/                     Sujet + conception (MCD / diagramme de classes dans doc/conception/)
database/                Scripts SQL : création des tables, données de test
jee-app/                 Version 1 : Servlets + JSP + Hibernate/JDBC (Tomcat)
  src/main/java/fr/cytech/assurance/
    model/               Entités (Client, Contrat, Garantie, Sinistre, Paiement, ...)
    model/enums/         Statuts (StatutContrat, StatutSinistre, ...) et rôles
    dao/                 Accès aux données (Hibernate / JDBC)
    service/             Logique métier
    controller/          Servlets
    filter/              Filtres (authentification, contrôle des rôles)
    ai/                  Client de l'API d'IA (assistant)
    util/                Outils partagés (validation des saisies, config Hibernate, ...)
  src/main/resources/    hibernate.cfg.xml, fichiers de configuration
  src/main/webapp/
    WEB-INF/views/       JSP par profil : auth/, client/, agent/, admin/, common/
    assets/              css/, js/, img/
```

## Base de données (MySQL)
Chacun utilise sa propre base MySQL en local.

### 1. Installer MySQL (Ubuntu / Debian)
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl enable --now mysql
```

### 2. Créer la base et l'utilisateur
```bash
sudo mysql
```
```sql
CREATE DATABASE assurance CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'assurance_user'@'localhost' IDENTIFIED BY 'un_mot_de_passe';
GRANT ALL PRIVILEGES ON assurance.* TO 'assurance_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 3. Configurer l'accès
```bash
cp .env.example .env
```
Puis renseigner dans `.env` :
```
DB_URL=jdbc:mysql://localhost:3306/assurance
DB_USER=assurance_user
DB_PASSWORD=un_mot_de_passe
```
Le fichier `.env` n'est pas versionné : il contient les mots de passe et la clé de l'API d'IA (`AI_API_KEY`).

### 4. Se connecter a la base de donnée
```bash
mysql -u assurance_user -p assurance < database/schema.sql
mysql -u assurance_user -p assurance < database/data.sql
```

