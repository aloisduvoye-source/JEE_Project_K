-- schema.sql
-- Structure de la base "assurance" : création de toutes les tables
-- (clés primaires, clés étrangères, contraintes).
-- À exécuter en premier sur une base vide.
-- Toute modification d'une table doit être reportée ici.


DROP TABLE IF EXISTS client;

CREATE TABLE IF NOT EXISTS client (
    id          SERIAL PRIMARY KEY,
    nom         VARCHAR(50) NOT NULL,
    prenom      VARCHAR(50) NOT NULL
)
