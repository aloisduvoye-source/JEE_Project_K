# JEE_Project_K
Le projet de JEE du groupe K : plateforme web de gestion d'assurance (clients, contrats, garanties, sinistres, paiements, assistant IA, statistiques).

- Sujet : [doc/projet2026_2027.pdf](doc/projet2026_2027.pdf)
- Avancement : [doc/fonctionnalites.md](doc/fonctionnalites.md)
- Règles de travail (Git, conventions) : [CONTRIBUTING.md](CONTRIBUTING.md)

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
mysql -u assurance_user -p assurance
```


## Lancer l'application (Tomcat)

### Prérequis
- Java 17 ou plus : `java -version`
- Maven : `mvn -version`
- Tomcat **10.1** (Servlet 6.0 / Jakarta EE 10)

### 1. Installer Tomcat (une seule fois)
```bash
cd ~
curl -O https://repo1.maven.org/maven2/org/apache/tomcat/tomcat/10.1.61/tomcat-10.1.61.tar.gz
tar -xzf tomcat-10.1.61.tar.gz && rm tomcat-10.1.61.tar.gz
```
Tomcat est alors installé dans `~/apache-tomcat-10.1.61`.

Facultatif : ajouter deux raccourcis à la fin de `~/.zshrc` (ou `~/.bashrc`), puis `source ~/.zshrc` :
```bash
export CATALINA_HOME="$HOME/apache-tomcat-10.1.61"
alias tomcat-start='"$CATALINA_HOME/bin/startup.sh"'
alias tomcat-stop='"$CATALINA_HOME/bin/shutdown.sh"'
```

### 2. Construire et déployer l'application
Depuis la racine du dépôt :
```bash
mvn -f jee-app/pom.xml package                               # produit jee-app/target/assurance.war
cp jee-app/target/assurance.war ~/apache-tomcat-10.1.61/webapps/
```

### 3. Démarrer Tomcat
```bash
~/apache-tomcat-10.1.61/bin/startup.sh                       # ou : tomcat-start
```
L'application est accessible sur **http://localhost:8080/assurance/**

Après une modification du code : relancer les commandes de l'étape 2. Tomcat redéploie automatiquement le nouveau `.war`, pas besoin de le redémarrer.

### 4. Arrêter Tomcat
```bash
~/apache-tomcat-10.1.61/bin/shutdown.sh                      # ou : tomcat-stop
```

### En cas de problème
- Les erreurs sont dans `~/apache-tomcat-10.1.61/logs/catalina.<date>.log`.
- `Adresse déjà utilisée` dans les logs : un autre programme occupe le port 8080. Le trouver avec `ss -ltnp | grep 8080`.
- Page 404 : vérifier l'URL (`/assurance/` à la fin) et que `assurance.war` est bien dans `webapps/`.
