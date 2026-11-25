# JEE_Project_K
Projet JEE du groupe K : plateforme web de gestion d'assurance (clients, contrats, garanties, sinistres, paiements, assistant IA, statistiques).

Sujet : [doc/projet2026_2027.pdf](doc/projet2026_2027.pdf)

| Dossier | Version |
|---|---|
| `jee-app/` | Version 1 : Servlets + JSP + Hibernate |
| `spring-app/` | Version 2 : Spring Boot + JSP + Spring Data JPA |

Les deux versions utilisent la même base MySQL, le même `.env` et la même adresse.

## Démarrage rapide

**Prérequis** : Java 17 ou plus, Maven et Docker avec Docker Compose (voir [Installer Docker](#installer-docker)).

**1. Configurer** (une seule fois) :
```bash
cp .env.example .env
```
Puis remplir dans `.env` :
- `DB_USER` et `DB_PASSWORD` : utilisateur MySQL créé dans le conteneur (pas `root`) ;
- `AI_API_KEY` : clé Google Gemini pour l'assistant, à créer sur https://aistudio.google.com/api-keys (facultatif : sans clé, l'assistant indique qu'il n'est pas configuré).

Le `.env` n'est jamais commité.

**2. Lancer**, depuis la racine du dépôt :
```bash
# Version 1 (par défaut)
mvn -f jee-app/pom.xml clean package
docker compose up -d --build

# Version 2
mvn -f spring-app/pom.xml clean package
APP=spring-app docker compose up -d --build
```
Pour utiliser `spring-app` par défaut, ajouter `APP=spring-app` dans le `.env`.

**3. Ouvrir http://localhost:8080/** avec un compte de test :

| Rôle | Identifiant | Mot de passe |
|---|---|---|
| Administrateur | `admin` | `admin123` |
| Agent | `agent` | `agent123` |
| Client | `client` | `client123` |

Au premier lancement, MySQL crée les tables (`database/schema.sql`) et insère les données de test (`database/dump.sql`).

## Au quotidien

**Après une modification du code Java ou des JSP** : reconstruire le `.war` puis Tomcat seulement.
```bash
mvn -f jee-app/pom.xml clean package               # ou spring-app/pom.xml
docker compose up -d --build tomcat                # avec APP=spring-app pour la version 2
```
Toujours utiliser `clean` : sinon une classe supprimée ou renommée peut rester dans le `.war`.

**Après une modification de `schema.sql` ou `dump.sql`** (ou un `git pull` qui les modifie) : les scripts ne s'exécutent qu'à la création de la base, il faut la recréer.
```bash
docker compose down -v
docker compose up -d --build
```

**Lancer spring-app sans Docker pour Tomcat** (MySQL seul dans Docker), depuis `spring-app/` :
```bash
docker compose up -d db
DB_URL=jdbc:mysql://localhost:3307/assurance mvn spring-boot:run
```

| Commande Docker | Effet |
|---|---|
| `docker compose up -d --build` | lance (ou relance) MySQL et Tomcat en arrière-plan |
| `docker compose ps` | état des conteneurs |
| `docker compose logs -f tomcat` | journaux de Tomcat en direct (`Ctrl+C` pour quitter) |
| `docker compose down` | arrête les conteneurs, **les données sont conservées** |
| `docker compose down -v` | arrête et **supprime la base et les pièces justificatives** |

### Branches et Pull Requests
On ne travaille jamais directement sur `main` : une mission = une branche, qui revient par une Pull Request relue par un autre membre.
```bash
git switch main && git pull origin main
git switch -c nom_de_la_branche
# ... travailler, puis vérifier que ça compile avant chaque commit (BUILD SUCCESS) :
mvn -f jee-app/pom.xml clean package               # ou spring-app/pom.xml
git push -u origin nom_de_la_branche               # puis ouvrir la Pull Request sur GitHub
```

## Organisation du code

```
doc/                     Sujet et conception (MCD, diagramme de classes)
database/                schema.sql (tables) et dump.sql (données de test)
jee-app/src/main/
  java/fr/cytech/assurance/
    model/, model/enums/ Entités, statuts et rôles
    dao/                 Accès aux données (Hibernate)
    service/             Règles métier, appelées par les servlets
    controller/          Servlets
    filter/              Contrôle d'accès par rôle (RoleFiltre)
    listener/            Démarrage et arrêt (Hibernate, mise à jour des statuts)
    exception/           Exceptions métier
    ai/                  Client de l'API d'IA (assistant)
    util/                Validation des saisies, connexion JPA, stockage des pièces...
  resources/             persistence.xml, connaissances de l'assistant
  webapp/WEB-INF/views/  JSP par profil : auth/, compte/, client/, agent/, admin/, common/
spring-app/src/main/
  java/fr/cytech/assurance/
    model/, exception/   Identiques à jee-app
    repository/          Accès aux données (Spring Data JPA, remplace dao/)
    service/             Règles métier (@Service, @Transactional)
    controller/          Contrôleurs Spring MVC (remplacent les servlets)
    web/                 Contrôle d'accès par rôle (RoleIntercepteur, remplace RoleFiltre)
    config/              Configuration Spring : .env, page 403, intercepteur, mise à jour des statuts
    ai/, util/           Comme dans jee-app, en composants Spring
  resources/             application.properties (remplace persistence.xml et web.xml)
  webapp/WEB-INF/views/  Mêmes JSP que jee-app
```

## Fonctionnement

### Contrôle d'accès
Chaque rôle a son espace, protégé d'après le début de l'URL :

| Préfixe d'URL | Rôles autorisés | Page d'accueil |
|---|---|---|
| `/client/...` | CLIENT | `/client/accueil` |
| `/agent/...` | AGENT, ADMIN | `/agent/accueil` |
| `/admin/...` | ADMIN | `/admin/accueil` |
| `/compte/...` | tout utilisateur connecté | `/compte` |

Non connecté : redirection vers `/connexion`. Mauvais rôle : page « Accès refusé » (403). Après la connexion, chacun arrive sur l'accueil de son rôle.

**Pour toute nouvelle page**, l'URL doit commencer par le préfixe du rôle concerné (par exemple `/client/contrats`) : elle est alors protégée sans code de sécurité dans le contrôleur. Une page sans préfixe est accessible à tous.

### Pièces justificatives
Les fichiers envoyés avec un sinistre (PDF, JPG ou PNG, 5 Mo maximum chacun) sont stockés dans le volume Docker `pieces_data` (`/data/pieces`, variable `DOSSIER_PIECES`), et non dans le `.war`, qui est écrasé à chaque déploiement. La base ne garde que leur nom d'origine et leur nom aléatoire sur le disque.

Ils ne sont jamais servis directement : `/client/sinistres/piece?id=...` et `/agent/sinistres/piece?id=...` vérifient les droits de l'utilisateur.

## Base de données

**Console MySQL** (mot de passe : `DB_PASSWORD`) :
```bash
docker exec -it mysql_db mysql -u assurance_user -p assurance
```
**Interface graphique** (DBeaver, MySQL Workbench...) : hôte `127.0.0.1` (et non `localhost`), port **3307**, base `assurance`, identifiants du `.env`. Le port 3307 évite le conflit avec un MySQL installé sur la machine.

### Modifier les données de test (`dump.sql`)
- **À la main** : écrire les `INSERT` dans `database/dump.sql`, dans l'ordre des clés étrangères (utilisateurs et types d'assurance, puis contrats, puis sinistres et paiements).
- **Par export** : remplir la base (via l'application ou un outil graphique), puis exporter uniquement les données :
  ```bash
  docker exec mysql_db sh -c 'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --no-create-info --skip-triggers --complete-insert --no-tablespaces assurance' > database/dump.sql
  ```
  (`--no-create-info` : la structure reste dans `schema.sql` ; `--complete-insert` : nom des colonnes dans chaque `INSERT` ; `--no-tablespaces` : évite une erreur de droits avec MySQL 8.)

Après une modification d'une table, mettre à jour `schema.sql` **et** vérifier que `dump.sql` se charge toujours (`docker compose down -v` puis `up`) avant de pousser.

## En cas de problème
| Message | Solution |
|---|---|
| `permission denied while trying to connect to the docker API` | La session n'a pas encore le groupe `docker` : se déconnecter puis se reconnecter. |
| `port is already allocated` | Un autre programme utilise le port 8080 ou 3307 (un Tomcat local par exemple) : `ss -ltnp \| grep 8080`. |
| `assurance.war: not found` pendant le `build` | Construire d'abord le `.war` : `mvn -f jee-app/pom.xml clean package` (ou `spring-app/pom.xml` avec `APP=spring-app`). |
| `Access denied for user` ou `Schema-validation` dans `docker compose logs tomcat` | La base ne correspond pas au `.env` ou à `schema.sql` : `docker compose down -v` puis `docker compose up -d --build`. |

## Installer Docker

**Windows / macOS** : installer [Docker Desktop](https://www.docker.com/products/docker-desktop/), qui contient Docker Compose.

**Ubuntu / Linux Mint** :
```bash
sudo apt update
sudo apt install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$UBUNTU_CODENAME") stable" \
| sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Utiliser Docker sans sudo, puis se déconnecter et se reconnecter
sudo usermod -aG docker $USER
```
Sous Linux Mint, il faut bien `UBUNTU_CODENAME` (et non `VERSION_CODENAME`, qui donne le nom de version de Mint).

**Vérifier** :
```bash
docker --version
docker compose version
docker run --rm hello-world      # doit afficher « Hello from Docker! »
```
