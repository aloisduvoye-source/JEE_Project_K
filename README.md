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
    service/             Logique métier (règles, hachage des mots de passe...), appelée par les servlets
    controller/          Servlets
    filter/              Filtre d'accès par rôle (RoleFiltre)
    listener/            Actions au démarrage et à l'arrêt de l'application (initialisation d'Hibernate)
    exception/           Exceptions métier (ex. UtilisateurDejaExistantException)
    ai/                  Client de l'API d'IA (assistant)
    util/                Outils partagés (validation des saisies, connexion JPA, ...)
  src/main/resources/    META-INF/persistence.xml (configuration Hibernate / JPA)
  src/main/webapp/
    WEB-INF/views/       JSP par profil : auth/, compte/, client/, agent/, admin/, common/
    assets/              css/, js/, img/
```

## Travailler à plusieurs (branches et Pull Requests)

On ne travaille jamais directement sur `main` : chaque mission part de `main` à jour dans sa propre branche, puis revient via une Pull Request relue par un autre membre.

```bash
# 1. Repartir de main à jour et créer sa branche (une mission = une branche)
git switch main
git pull origin main
git switch -c nom_de_la_branche

# 2. Travailler, puis committer. Ne JAMAIS committer sans compiler :
mvn -f jee-app/pom.xml clean package    # doit afficher BUILD SUCCESS

# 3. Publier la branche puis ouvrir la Pull Request sur GitHub
git push -u origin nom_de_la_branche
```

## Prérequis
- **Java 17** ou plus (`java -version`) et **Maven** (`mvn -version`) : pour construire l'application
- **Docker** avec **Docker Compose** : MySQL et Tomcat tournent dans des conteneurs, rien d'autre à installer

## Installer Docker (une seule fois)

### Ubuntu / Linux Mint
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

# Utiliser Docker sans sudo
sudo usermod -aG docker $USER
```
Se **déconnecter de sa session puis se reconnecter** (ou redémarrer) pour que le groupe `docker` soit pris en compte.

> Sous Linux Mint, il faut bien `UBUNTU_CODENAME` (et non `VERSION_CODENAME`, qui donne le nom de version de Mint).

### Windows / macOS
Installer [Docker Desktop](https://www.docker.com/products/docker-desktop/), qui contient déjà Docker Compose.

### Vérifier
```bash
docker --version
docker compose version
docker run --rm hello-world      # doit afficher « Hello from Docker! »
```

## Configurer l'accès (une seule fois)
```bash
cp .env.example .env
```
Puis renseigner dans `.env` :
```
DB_USER=assurance_user
DB_PASSWORD=un_mot_de_passe
AI_API_KEY=votre_cle_gemini
```
Ces valeurs servent à créer l'utilisateur MySQL du conteneur et à connecter l'application. `DB_USER` ne doit **pas** être `root`.

`AI_API_KEY` est la clé de l'API Google Gemini, utilisée par l'assistant IA. Pour la récupérer, se connecter avec un compte Google sur **https://aistudio.google.com/api-keys** et créer une clé. Sans clé, l'application fonctionne mais l'assistant affiche qu'il n'est pas configuré.
Le fichier `.env` n'est pas versionné : il contient les mots de passe et la clé de l'API d'IA (`AI_API_KEY`).

## Lancer l'application
Depuis la racine du dépôt :
```bash
mvn -f jee-app/pom.xml clean package # construit jee-app/target/assurance.war
docker compose up -d --build         # construit l'image Tomcat, lance MySQL puis Tomcat
```
Au premier lancement, MySQL crée automatiquement les tables (`database/schema.sql`) et insère les données de test (`database/dump.sql`).

L'application est accessible sur **http://localhost:8080/**

Comptes de test :

| Rôle | Identifiant | Mot de passe |
|---|---|---|
| Administrateur | `admin` | `admin123` |
| Agent | `agent` | `agent123` |
| Client | `client` | `client123` |

> Toujours utiliser `clean package` : sans `clean`, une classe supprimée ou renommée peut rester dans le `.war` et provoquer des erreurs incompréhensibles.

## Tuto Docker rapide

| Commande | Effet |
|---|---|
| `docker compose up -d --build` | lance (ou relance) MySQL et Tomcat en arrière-plan |
| `docker compose ps` | état des conteneurs |
| `docker compose logs -f tomcat` | journaux de Tomcat en direct (`Ctrl+C` pour quitter) |
| `docker compose down` | arrête les conteneurs, **les données sont conservées** |
| `docker compose down -v` | arrête et **supprime la base et les pièces justificatives** (repart de `schema.sql` + `dump.sql` au prochain lancement) |

**Après une modification du code Java ou des JSP :**
```bash
mvn -f jee-app/pom.xml clean package
docker compose up -d --build tomcat
```

**Après une modification de `schema.sql` ou `dump.sql`** (ou après un `git pull` qui les modifie) : les scripts ne sont exécutés qu'à la création de la base, il faut donc la recréer.
```bash
docker compose down -v
docker compose up -d --build
```

## Pièces justificatives
Les fichiers envoyés avec un sinistre (PDF, JPG ou PNG, 5 Mo maximum chacun) sont enregistrés dans le volume Docker `pieces_data`, monté sur `/data/pieces` dans le conteneur Tomcat (variable `DOSSIER_PIECES`), et non dans le `.war`, qui est écrasé à chaque déploiement. La base ne garde que leur nom d'origine et leur nom sur le disque (un identifiant aléatoire).

Ils ne sont jamais servis directement : `/client/sinistres/piece?id=...` et `/agent/sinistres/piece?id=...` vérifient que l'utilisateur a le droit de les voir.

## Contrôle d'accès
Chaque rôle a son espace. Les URL qui commencent par ces préfixes sont protégées automatiquement par le filtre `RoleFiltre` :

| Préfixe d'URL | Rôles autorisés | Page d'accueil |
|---|---|---|
| `/client/...` | CLIENT | `/client/accueil` |
| `/agent/...` | AGENT, ADMIN | `/agent/accueil` |
| `/admin/...` | ADMIN | `/admin/accueil` |
| `/compte/...` | tout utilisateur connecté | `/compte` |

- Non connecté : redirection vers `/connexion`. Mauvais rôle : page « Accès refusé » (erreur 403).
- Après la connexion, chacun est redirigé vers la page d'accueil de son rôle.

**Règle pour toute nouvelle page** : son URL doit commencer par le préfixe du rôle concerné, par exemple `@WebServlet("/client/contrats")`. Elle est alors protégée sans aucun code de sécurité dans la servlet. Une page sans préfixe est accessible à tout le monde.

## Base de données

### Se connecter à la base
Console MySQL dans le conteneur (le mot de passe est `DB_PASSWORD`) :
```bash
docker exec -it mysql_db mysql -u assurance_user -p assurance
```
Depuis une interface graphique (DBeaver, MySQL Workbench...) : hôte `127.0.0.1`, port **3307**, base `assurance`, identifiants du `.env`.

> Le port 3307 évite le conflit avec un MySQL éventuellement installé sur la machine (3306). Utiliser `127.0.0.1` et non `localhost`.

### Générer les données de test (`dump.sql`)
Deux façons de produire `dump.sql` :

**a. À la main** : écrire directement les `INSERT` dans `database/dump.sql`, en respectant l'ordre des clés étrangères (d'abord les utilisateurs et les types d'assurance, puis les contrats, puis les sinistres et les paiements).

**b. Export depuis la base du conteneur** : remplir la base (via l'application ou un outil graphique), puis exporter uniquement les données :
```bash
docker exec mysql_db sh -c 'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --no-create-info --skip-triggers --complete-insert --no-tablespaces assurance' > database/dump.sql
```
- `--no-create-info` : n'exporte que les données ; la structure reste dans `schema.sql`
- `--complete-insert` : écrit le nom des colonnes dans chaque `INSERT`, plus lisible et plus robuste si une colonne est ajoutée
- `--no-tablespaces` : évite une erreur de droits avec MySQL 8

> Si vous modifiez une table, mettez à jour `schema.sql` **et** vérifiez que `dump.sql` se charge toujours dessus (`docker compose down -v` puis `up`) avant de pousser.

## En cas de problème
- `permission denied while trying to connect to the docker API` : la session n'a pas encore le groupe `docker`. Se déconnecter puis se reconnecter.
- `port is already allocated` : un autre programme utilise le port 8080 ou 3307 (par exemple un Tomcat installé localement). Le trouver avec `ss -ltnp | grep 8080`.
- `assurance.war: not found` pendant le `build` : lancer d'abord `mvn -f jee-app/pom.xml clean package`.
- `Access denied for user` ou `Schema-validation` dans `docker compose logs tomcat` : la base ne correspond pas au `.env` ou à `schema.sql`. La recréer avec `docker compose down -v` puis `docker compose up -d --build`.
