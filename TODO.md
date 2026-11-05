# TODO — Version 1 (Servlets + JSP + Hibernate)

Une fonctionnalité = une branche = un service + ses servlets + ses JSP, testée de bout en bout avant la PR.
Les DAO existent déjà pour toutes les entités : il reste surtout la couche service et l'interface.
Rappel du sujet : **toutes les saisies doivent être vérifiées par les servlets** (utiliser `Validateur`) avant l'enregistrement.

Les URL suivent les préfixes protégés par `RoleFiltre` : `/client/*`, `/agent/*` (agent + admin), `/admin/*`, `/compte/*`.

---

## 1. Sinistres (§2.5) — priorité haute

**Service**
- [ ] `SinistreService` : déclarer, lister par client / par contrat / par statut / par agent, détail
- [ ] Règles de transition de statut : `DECLARE → EN_ANALYSE → ACCEPTE | REFUSE`, puis `ACCEPTE → INDEMNISE` (refuser toute autre transition, ex. via une `SinistreInvalideException`)
- [ ] Déclaration possible uniquement sur un contrat `ACTIF` appartenant au client connecté, date du sinistre comprise dans la période du contrat et non future
- [ ] Indemnisation : montant > 0, uniquement si statut `ACCEPTE`
- [ ] Ajout de commentaires (`CommentaireSinistreDao`) par l'agent

**Client**
- [ ] `/client/sinistres` : historique des sinistres du client avec leur statut
- [ ] `/client/sinistres/nouveau` : formulaire de déclaration (contrat, date, type, description, montant estimé)
- [ ] `/client/sinistres/detail?id=` : suivi de l'état, commentaires, pièces (vérifier que le sinistre appartient bien au client)
- [ ] Envoi de pièces justificatives (`@MultipartConfig`, contrôle du type et de la taille des fichiers, stockage hors du dossier webapp)
- [ ] Téléchargement d'une pièce, uniquement par son propriétaire ou un agent/admin

**Agent**
- [ ] `/agent/sinistres` : liste avec filtre par statut, « mes sinistres » (agent affecté)
- [ ] `/agent/sinistres/detail?id=` : s'affecter le sinistre, changer le statut, commenter, saisir l'indemnisation

---

## 2. Contrats (§2.2) — compléter l'existant

Déjà fait : création (agent), liste agent, « Mes contrats » client.

- [ ] `/agent/contrats/detail?id=` et `/client/contrats/detail?id=` : consulter un contrat (garanties, paiements, sinistres)
- [ ] Modifier un contrat (dates, prime, garanties) — seulement si `EN_ATTENTE` ou `ACTIF`
- [ ] Rechercher un contrat (par n°, client, type, statut)
- [ ] Activer / suspendre / réactiver un contrat (`ContratDao.changerStatut`) avec règles de transition dans `ContratService`
- [ ] Résilier (`ContratDao.resilier`) : motif obligatoire, statut final
- [ ] Renouveler : créer un nouveau contrat avec `contratPrecedent` renseigné, mêmes garanties, nouvelles dates (méthode à ajouter dans `ContratDao` / `ContratService`)
- [ ] Passage automatique en `EXPIRE` des contrats dont la date de fin est dépassée (au démarrage dans `DemarrageApplication`, ou à la consultation)

---

## 3. Paiements (§2.6)

- [ ] `PaiementService` : génération des échéances à l'activation d'un contrat (à vérifier si ce n'est pas déjà fait à la création)
- [ ] Marquer les paiements en retard (`PaiementDao.marquerEnRetard`), par ex. au démarrage
- [ ] `/client/paiements` : paiements effectués, en attente, échéances à venir, historique
- [ ] `/agent/paiements` : liste par contrat, paiements en retard
- [ ] Enregistrer un paiement (date, mode de paiement) et annuler un paiement

---

## 4. Types d'assurance et garanties (§2.3, §2.4)

- [ ] `GarantieService` : créer, modifier, supprimer (refuser si utilisée par un contrat), nom unique
- [ ] `/agent/garanties` : CRUD des garanties
- [ ] Compléter `TypeAssuranceService` : créer, modifier, supprimer (refuser si utilisé), ajouter / retirer une garantie disponible
- [ ] `/admin/types-assurance` : CRUD des types et de leurs garanties disponibles
- [ ] `/client/garanties` (ou dans le détail d'un contrat) : consulter ses garanties
- [ ] Vérifier que les données de test contiennent les 5 types (auto, habitation, santé, voyage, professionnelle) et les 6 garanties du sujet

---

## 5. Administration des utilisateurs (§2.1)

Règle : **un seul administrateur**, créé par `dump.sql`. La base l'impose avec l'index unique `uk_utilisateur_admin_unique` (`schema.sql`) ; l'application doit le vérifier avant pour afficher un message clair.

- [ ] `/admin/utilisateurs` : liste filtrable par rôle (`UtilisateurDao.listerParRole`)
- [ ] Créer un agent (pas de création d'administrateur)
- [ ] Activer / désactiver un compte (`UtilisateurDao.changerEtat`) et refuser la connexion d'un compte désactivé ; l'admin ne peut pas être désactivé
- [ ] Changer le rôle d'un utilisateur entre `CLIENT` et `AGENT` uniquement : le rôle `ADMIN` ne peut être ni attribué ni retiré
- [ ] Interdire la suppression du compte administrateur
- [ ] `UtilisateurDao.compterParRole(Role)` + vérification dans le service (exception métier si un 2ᵉ admin serait créé), en plus de l'index en base
- [ ] `/agent/clients` : liste et recherche des clients (`ClientDao.rechercher`), fiche client avec ses contrats

---

## 6. Statistiques et tableau de bord (§2.8)

- [ ] `StatistiqueDao` : requêtes d'agrégation (COUNT / SUM, GROUP BY statut, type, mois)
- [ ] Clients : total, nouveaux clients (par mois), répartition par type d'assurance
- [ ] Contrats : actifs, expirés, résiliés, évolution mensuelle
- [ ] Finances : chiffre d'affaires (paiements effectués), total des primes, total des indemnisations, évolution des revenus
- [ ] `/agent/statistiques` et `/admin/statistiques` (indicateurs globaux / de performance pour l'admin)
- [ ] Graphiques avec Chart.js

---

## 7. Assistant IA (§2.7)

- [ ] Client HTTP vers l'API d'IA dans le package `ai/` (clé lue depuis `AI_API_KEY` dans `.env`)
- [ ] Construction du contexte envoyé : contrats, garanties et sinistres du client connecté et derniers messages (`MessageAssistantDao.listerDerniers`)
- [ ] `/client/assistant` : page de discussion, historique enregistré, bouton pour effacer l'historique
- [ ] Gérer les erreurs (API indisponible, délai dépassé) sans faire planter la page
- [ ] Tester les questions du sujet : contrats actifs, garanties de l'assurance auto, déclarer un sinistre, documents à fournir, différence entre deux garanties, état du dernier sinistre

---

## 8. Transversal

- [ ] Menus de navigation par rôle dans `header.jspf` (liens vers toutes les nouvelles pages)
- [ ] Feuille de style commune dans `assets/css/`
- [ ] Messages de succès / d'erreur cohérents sur toutes les pages
- [ ] Protection contre les accès par id d'un autre utilisateur (un client ne voit que ses données)
- [ ] Tests unitaires des services (règles de statut, validations) dans `src/test`
- [ ] Mettre à jour `database/dump.sql` avec des données de démo pour chaque fonctionnalité
- [ ] Mettre à jour le MCD / diagramme de classes dans `doc/conception/`

---

## 9. Version 2 — Spring Boot (à faire une fois la V1 stable)

- [ ] Nouveau module Spring Boot (Spring MVC + Spring Data JPA + Thymeleaf ou JSP)
- [ ] Reprendre les entités, réécrire les DAO en repositories, réutiliser la logique des services
- [ ] Spring Security pour l'authentification et les rôles
- [ ] Mêmes fonctionnalités que la V1
