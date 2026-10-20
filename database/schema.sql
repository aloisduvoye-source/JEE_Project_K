-- schema.sql
-- Structure de la base "assurance" : création de toutes les tables
-- (clés primaires, clés étrangères, contraintes).
-- À exécuter en premier sur une base vide.
-- Toute modification d'une table doit être reportée ici.

DROP TABLE IF EXISTS utilisateur;
DROP TABLE IF EXISTS client;

CREATE TABLE utilisateur(
    id                 SERIAL PRIMARY KEY,
    nom                     VARCHAR(100) NOT NULL,
    prenom                  VARCHAR(100) NOT NULL,
    email                   VARCHAR(180) NOT NULL,
    identifiant             VARCHAR(100) NOT NULL,
    mot_de_passe_hash       VARCHAR(255) NOT NULL,
    role                    ENUM('CLIENT', 'AGENT', 'ADMIN') NOT NULL
);

CREATE TABLE client(
    id                      SERIAL PRIMARY KEY,
    utilisateur_id          UNIQUE INTEGER REFERENCES utilisateur(id) NOT NULL,
    telephone               VARCHAR(255),
    adresse                 VARCHAR(255),
    date_naissance          DATE NULL
);





