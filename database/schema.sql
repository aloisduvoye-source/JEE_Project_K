-- schema.sql
-- Structure de la base "assurance" : création de toutes les tables
-- (clés primaires, clés étrangères, contraintes).
-- À exécuter en premier sur une base vide.
-- Toute modification d'une table doit être reportée ici.

DROP TABLE IF EXISTS contrat_garantie;
DROP TABLE IF EXISTS type_assurance_garantie;
DROP TABLE IF EXISTS garantie;
DROP TABLE IF EXISTS utilisateur;
DROP TABLE IF EXISTS client;
DROP TABLE IF EXISTS contrat;
DROP TABLE IF EXISTS type_assurance;

CREATE TABLE IF NOT EXISTS utilisateur(
    id                      SERIAL PRIMARY KEY,
    nom                     VARCHAR(100) NOT NULL,
    prenom                  VARCHAR(100) NOT NULL,
    email                   VARCHAR(180) NOT NULL,
    identifiant             VARCHAR(100) NOT NULL,
    mot_de_passe_hash       VARCHAR(255) NOT NULL,
    role                    ENUM('CLIENT', 'AGENT', 'ADMIN') NOT NULL
);

CREATE TABLE IF NOT EXISTS client(
    id                      SERIAL PRIMARY KEY,
    id_utilisateur          UNIQUE INTEGER REFERENCES utilisateur(id) NOT NULL ON DELETE CASCADE,
    telephone               VARCHAR(255),
    adresse                 VARCHAR(255),
    date_naissance          DATE NULL
);

CREATE TABLE IF EXISTS contrat(
    id                      SERIAL PRIMARY KEY,
    id_client               INTEGER REFERENCES client(id),
    id_type_assurance       INTEGER REFERENCES type_assurance(id),
    numero                  BIGINT,
    date_debut              DATE,
    date_fin                DATE,
    prime                   DECIMAL(12,2)
    statut                  ENUM('EN_ATTENTE', 'ACTIF', 'SUSPENDU', 'EXPIRE', 'RESILIE') NOT NULL DEFAULT 'EN_ATTENTE',
);


CREATE TABLE IF EXISTS type_assurance(
    id                      SERIAL PRIMARY KEY,
    nom                     VARCHAR(100),
    description             TEXT,
    prix_base               DECIMAL(2,12)
);

CREATE TABLE IF NOT EXISTS garantie(
    id                      SERIAL PRIMARY KEY,
    nom                     VARCHAR(100) NOT NULL UNIQUE,
    description             TEXT
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
