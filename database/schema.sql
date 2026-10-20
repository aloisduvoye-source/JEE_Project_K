-- schema.sql
-- Structure de la base "assurance" : création de toutes les tables
-- (clés primaires, clés étrangères, contraintes).
-- À exécuter en premier sur une base vide.
-- Toute modification d'une table doit être reportée ici.

DROP TABLE IF EXISTS message_assistant;
DROP TABLE IF EXISTS paiement;
DROP TABLE IF EXISTS commentaire_sinistre;
DROP TABLE IF EXISTS piece_justificative;
DROP TABLE IF EXISTS sinistre;
DROP TABLE IF EXISTS contrat_garantie;
DROP TABLE IF EXISTS type_assurance_garantie;
DROP TABLE IF EXISTS garantie;
DROP TABLE IF EXISTS contrat;
DROP TABLE IF EXISTS client;
DROP TABLE IF EXISTS utilisateur;
DROP TABLE IF EXISTS type_assurance;

CREATE TABLE IF NOT EXISTS utilisateur(
    id                      SERIAL PRIMARY KEY,
    nom                     VARCHAR(100) NOT NULL,
    prenom                  VARCHAR(100) NOT NULL,
    email                   VARCHAR(180) NOT NULL UNIQUE,
    identifiant             VARCHAR(100) NOT NULL UNIQUE,
    mot_de_passe_hash       VARCHAR(255) NOT NULL,
    role                    ENUM('CLIENT', 'AGENT', 'ADMIN') NOT NULL,
    actif                   BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS client(
    id                      SERIAL PRIMARY KEY,
    id_utilisateur          BIGINT UNSIGNED NOT NULL UNIQUE,
    telephone               VARCHAR(255),
    adresse                 VARCHAR(255),
    date_naissance          DATE NULL,
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS type_assurance(
    id                      SERIAL PRIMARY KEY,
    nom                     VARCHAR(100) NOT NULL UNIQUE,
    description             TEXT,
    prix_base               DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_type_assurance_prix CHECK (prix_base >= 0)
);

CREATE TABLE IF NOT EXISTS contrat(
    id                      SERIAL PRIMARY KEY,
    id_client               BIGINT UNSIGNED NOT NULL,
    id_type_assurance       BIGINT UNSIGNED NOT NULL,
    numero                  BIGINT NOT NULL UNIQUE,
    date_debut              DATE NOT NULL,
    date_fin                DATE NOT NULL,
    prime                   DECIMAL(12,2) NOT NULL,
    statut                  ENUM('EN_ATTENTE', 'ACTIF', 'SUSPENDU', 'EXPIRE', 'RESILIE') NOT NULL DEFAULT 'EN_ATTENTE',
    date_resiliation        DATE NULL,
    motif_resiliation       VARCHAR(255) NULL,
    id_contrat_precedent    BIGINT UNSIGNED NULL,       -- contrat renouvelé par celui-ci
    CONSTRAINT chk_contrat_dates CHECK (date_fin > date_debut),
    CONSTRAINT chk_contrat_prime CHECK (prime > 0),
    CONSTRAINT chk_contrat_resiliation CHECK (statut <> 'RESILIE' OR date_resiliation IS NOT NULL),
    FOREIGN KEY (id_client) REFERENCES client(id),
    FOREIGN KEY (id_type_assurance) REFERENCES type_assurance(id),
    FOREIGN KEY (id_contrat_precedent) REFERENCES contrat(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS garantie(
    id                      SERIAL PRIMARY KEY,
    nom                     VARCHAR(100) NOT NULL UNIQUE,
    description             TEXT,
    plafond                 DECIMAL(12,2) NULL,         -- montant maximal remboursé
    franchise               DECIMAL(12,2) NULL,         -- montant restant à la charge du client
    CONSTRAINT chk_garantie_montants CHECK (plafond >= 0 AND franchise >= 0)
);

-- Garanties disponibles pour chaque type d'assurance
CREATE TABLE IF NOT EXISTS type_assurance_garantie(
    id_type_assurance       BIGINT UNSIGNED NOT NULL,
    id_garantie             BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_type_assurance, id_garantie),
    FOREIGN KEY (id_type_assurance) REFERENCES type_assurance(id) ON DELETE CASCADE,
    FOREIGN KEY (id_garantie) REFERENCES garantie(id) ON DELETE CASCADE
);

-- Garanties souscrites dans chaque contrat
-- (une garantie encore utilisée par un contrat ne peut pas être supprimée)
CREATE TABLE IF NOT EXISTS contrat_garantie(
    id_contrat              BIGINT UNSIGNED NOT NULL,
    id_garantie             BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_contrat, id_garantie),
    FOREIGN KEY (id_contrat) REFERENCES contrat(id) ON DELETE CASCADE,
    FOREIGN KEY (id_garantie) REFERENCES garantie(id)
);

CREATE TABLE IF NOT EXISTS sinistre(
    id                      SERIAL PRIMARY KEY,
    id_contrat              BIGINT UNSIGNED NOT NULL,
    numero                  BIGINT NOT NULL UNIQUE,
    date_sinistre           DATE NOT NULL,
    date_declaration        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description             TEXT NOT NULL,
    type_sinistre           ENUM('ACCIDENT', 'VOL', 'INCENDIE', 'DEGATS_DES_EAUX', 'MALADIE', 'AUTRE') NOT NULL,
    montant_estime          DECIMAL(12,2),
    statut                  ENUM('DECLARE', 'EN_ANALYSE', 'ACCEPTE', 'REFUSE', 'INDEMNISE') NOT NULL DEFAULT 'DECLARE',
    montant_indemnisation   DECIMAL(12,2),
    date_indemnisation      DATE NULL,
    id_agent                BIGINT UNSIGNED NULL,       -- agent qui traite le dossier
    CONSTRAINT chk_sinistre_montants CHECK (montant_estime >= 0 AND montant_indemnisation >= 0),
    CONSTRAINT chk_sinistre_indemnisation CHECK (statut <> 'INDEMNISE' OR (montant_indemnisation IS NOT NULL AND date_indemnisation IS NOT NULL)),
    FOREIGN KEY (id_contrat) REFERENCES contrat(id),
    FOREIGN KEY (id_agent) REFERENCES utilisateur(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS piece_justificative(
    id                      SERIAL PRIMARY KEY,
    id_sinistre             BIGINT UNSIGNED NOT NULL,
    nom_fichier             VARCHAR(255) NOT NULL,
    chemin                  VARCHAR(500) NOT NULL,
    type_mime               VARCHAR(100),
    date_ajout              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_sinistre) REFERENCES sinistre(id) ON DELETE CASCADE
);

-- Commentaires ajoutés par les agents lors de l'analyse d'un sinistre
CREATE TABLE IF NOT EXISTS commentaire_sinistre(
    id                      SERIAL PRIMARY KEY,
    id_sinistre             BIGINT UNSIGNED NOT NULL,
    id_auteur               BIGINT UNSIGNED NOT NULL,
    contenu                 TEXT NOT NULL,
    date_commentaire        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_sinistre) REFERENCES sinistre(id) ON DELETE CASCADE,
    FOREIGN KEY (id_auteur) REFERENCES utilisateur(id)
);

-- date_echeance : date à laquelle le paiement est dû ; date_paiement : renseignée une fois payé
CREATE TABLE IF NOT EXISTS paiement(
    id                      SERIAL PRIMARY KEY,
    id_contrat              BIGINT UNSIGNED NOT NULL,
    montant                 DECIMAL(12,2) NOT NULL,
    date_echeance           DATE NOT NULL,
    date_paiement           DATE NULL,
    mode_paiement           ENUM('CARTE', 'VIREMENT', 'PRELEVEMENT', 'CHEQUE') NULL,
    statut                  ENUM('EN_ATTENTE', 'EFFECTUE', 'EN_RETARD', 'ANNULE') NOT NULL DEFAULT 'EN_ATTENTE',
    CONSTRAINT chk_paiement_montant CHECK (montant > 0),
    CONSTRAINT chk_paiement_effectue CHECK (statut <> 'EFFECTUE' OR (date_paiement IS NOT NULL AND mode_paiement IS NOT NULL)),
    FOREIGN KEY (id_contrat) REFERENCES contrat(id)
);

-- Historique des conversations entre un client et l'assistant IA
CREATE TABLE IF NOT EXISTS message_assistant(
    id                      SERIAL PRIMARY KEY,
    id_utilisateur          BIGINT UNSIGNED NOT NULL,
    auteur                  ENUM('CLIENT', 'ASSISTANT') NOT NULL,
    contenu                 TEXT NOT NULL,
    date_message            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id) ON DELETE CASCADE
);
