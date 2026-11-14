-- dump.sql
-- Données de la base "assurance" : jeu de données de test
-- (utilisateurs, clients, contrats, garanties, sinistres, paiements...).
-- À exécuter après schema.sql.

-- Le fichier est en UTF-8 : sans cette ligne, un client MySQL réglé en latin1 abîme les accents
SET NAMES utf8mb4;

-- ============================================================
-- Données de référence imposées par le sujet (§2.3 et §2.4)
-- ============================================================

INSERT INTO type_assurance (nom, description, prix_base) VALUES
    ('Assurance automobile',      'Couverture des véhicules et de leurs conducteurs.',                       450.00),
    ('Assurance habitation',      'Couverture du logement, de son contenu et des occupants.',                250.00),
    ('Assurance santé',           'Prise en charge des frais médicaux et d''hospitalisation.',               600.00),
    ('Assurance voyage',          'Protection pendant les déplacements en France et à l''étranger.',          80.00),
    ('Assurance professionnelle', 'Couverture des locaux, du matériel et de l''activité d''une entreprise.',  900.00);

INSERT INTO garantie (nom, description, plafond, franchise) VALUES
    ('Responsabilité civile', 'Dommages causés à un tiers par l''assuré.',                        1000000.00, 0.00),
    ('Vol',                   'Vol ou tentative de vol des biens assurés.',                         20000.00, 150.00),
    ('Incendie',              'Dommages causés par un incendie, une explosion ou la foudre.',     300000.00, 300.00),
    ('Accident',              'Dommages corporels ou matériels suite à un accident.',               50000.00, 200.00),
    ('Dégâts des eaux',       'Dommages causés par une fuite, un débordement ou une infiltration.', 30000.00, 150.00),
    ('Assistance',            'Dépannage, rapatriement, aide d''urgence 24h/24.',                    5000.00, 0.00);

-- Garanties disponibles pour chaque type d'assurance
INSERT INTO type_assurance_garantie (id_type_assurance, id_garantie)
SELECT t.id, g.id
FROM type_assurance t
JOIN garantie g ON (t.nom, g.nom) IN (
    ('Assurance automobile',      'Responsabilité civile'),
    ('Assurance automobile',      'Vol'),
    ('Assurance automobile',      'Incendie'),
    ('Assurance automobile',      'Accident'),
    ('Assurance automobile',      'Assistance'),
    ('Assurance habitation',      'Responsabilité civile'),
    ('Assurance habitation',      'Vol'),
    ('Assurance habitation',      'Incendie'),
    ('Assurance habitation',      'Dégâts des eaux'),
    ('Assurance habitation',      'Assistance'),
    ('Assurance santé',           'Accident'),
    ('Assurance santé',           'Assistance'),
    ('Assurance voyage',          'Responsabilité civile'),
    ('Assurance voyage',          'Vol'),
    ('Assurance voyage',          'Accident'),
    ('Assurance voyage',          'Assistance'),
    ('Assurance professionnelle', 'Responsabilité civile'),
    ('Assurance professionnelle', 'Vol'),
    ('Assurance professionnelle', 'Incendie'),
    ('Assurance professionnelle', 'Dégâts des eaux'),
    ('Assurance professionnelle', 'Accident')
);

-- ============================================================
-- Comptes de test (un par rôle) : NE PAS utiliser en production
--   identifiant / mot de passe :  admin / admin123,  agent / agent123,  client / client123
-- Les mots de passe sont stockés hachés avec BCrypt
-- ============================================================

INSERT INTO utilisateur (nom, prenom, email, identifiant, mot_de_passe_hash, role) VALUES
    ('Doe',     'John', 'jdoe@assurance.test',      'admin',  '$2a$10$YFm72JBvHF.Urp1.v8ahluVoPj4MoE1mCJ.npHdJ6HuRb257QDIrm', 'ADMIN'),     -- mot de passe : admin123
    ('Marx',    'Karl', 'kmarx@assurance.test',     'agent',  '$2a$10$DgbZOJRickHvG7BFnh2VYuLrTojN9RvaspLF05n8hUfJjGZTtNzcG', 'AGENT'),     -- mot de passe : agent123
    ('Bill',    'Bob',  'bbill@assurance.test',     'client', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT');    -- mot de passe : client123

-- Le compte CLIENT a aussi sa fiche client
INSERT INTO client (id_utilisateur, telephone, adresse, date_naissance)
SELECT id, '0601020304', '12 rue des Lilas, 95000 Cergy', '1990-05-14'
FROM utilisateur WHERE identifiant = 'client';

-- ============================================================
-- Contrat de test déjà actif, pour pouvoir déclarer un sinistre tout de suite
--   assurance automobile du client de test, payée, commencée il y a 2 mois, avec toutes les garanties du type.
-- Dates relatives au jour du chargement : le contrat reste actif quelle que soit la date de création de la base.
-- ============================================================

INSERT INTO contrat (id_client, id_type_assurance, date_debut, date_fin, prime, statut)
SELECT c.id, t.id, CURDATE() - INTERVAL 2 MONTH, CURDATE() - INTERVAL 2 MONTH + INTERVAL 12 MONTH, 450.00, 'ACTIF'
FROM client c
JOIN utilisateur u ON u.id = c.id_utilisateur AND u.identifiant = 'client'
JOIN type_assurance t ON t.nom = 'Assurance automobile';

SET @contrat_test = LAST_INSERT_ID();

INSERT INTO contrat_garantie (id_contrat, id_garantie)
SELECT @contrat_test, tg.id_garantie
FROM type_assurance_garantie tg
JOIN type_assurance t ON t.id = tg.id_type_assurance AND t.nom = 'Assurance automobile';

-- Son paiement unique (la prime entière), réglé par carte la veille de la date de début
INSERT INTO paiement (id_contrat, montant, date_echeance, date_paiement, mode_paiement, statut)
SELECT id, prime, date_debut, date_debut - INTERVAL 1 DAY, 'CARTE', 'EFFECTUE'
FROM contrat WHERE id = @contrat_test;

-- ============================================================
-- Jeu de données de démonstration : agents, clients, contrats, paiements, sinistres...
-- Sert à tester les listes, les filtres et les statistiques avec des données variées.
--   - agents  : mot de passe agent123  (identifiants : mcurie, vhugo, gsand, lpasteur)
--   - clients : mot de passe client123 (identifiant : initiale du prénom + nom, ex. cmartin)
-- Toutes les dates sont relatives au jour du chargement (en jours avant aujourd'hui) :
-- les statuts restent cohérents quelle que soit la date de création de la base.
-- ============================================================

INSERT INTO utilisateur (nom, prenom, email, identifiant, mot_de_passe_hash, role, date_creation) VALUES
    ('Curie', 'Marie', 'marie.curie@assurance.test', 'mcurie', '$2a$10$DgbZOJRickHvG7BFnh2VYuLrTojN9RvaspLF05n8hUfJjGZTtNzcG', 'AGENT', NOW() - INTERVAL 460 DAY),
    ('Hugo', 'Victor', 'victor.hugo@assurance.test', 'vhugo', '$2a$10$DgbZOJRickHvG7BFnh2VYuLrTojN9RvaspLF05n8hUfJjGZTtNzcG', 'AGENT', NOW() - INTERVAL 563 DAY),
    ('Sand', 'George', 'george.sand@assurance.test', 'gsand', '$2a$10$DgbZOJRickHvG7BFnh2VYuLrTojN9RvaspLF05n8hUfJjGZTtNzcG', 'AGENT', NOW() - INTERVAL 657 DAY),
    ('Pasteur', 'Louis', 'louis.pasteur@assurance.test', 'lpasteur', '$2a$10$DgbZOJRickHvG7BFnh2VYuLrTojN9RvaspLF05n8hUfJjGZTtNzcG', 'AGENT', NOW() - INTERVAL 887 DAY);

-- Clients : compte (désactivé pour deux d'entre eux) puis fiche client
INSERT INTO utilisateur (nom, prenom, email, identifiant, mot_de_passe_hash, role, actif, date_creation) VALUES
    ('Martin', 'Camille', 'camille.martin@exemple.test', 'cmartin', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 549 DAY),
    ('Bernard', 'Lucas', 'lucas.bernard@exemple.test', 'lbernard', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 766 DAY),
    ('Dubois', 'Emma', 'emma.dubois@exemple.test', 'edubois', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 222 DAY),
    ('Thomas', 'Hugo', 'hugo.thomas@exemple.test', 'hthomas', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 319 DAY),
    ('Robert', 'Léa', 'lea.robert@exemple.test', 'lrobert', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 597 DAY),
    ('Richard', 'Nathan', 'nathan.richard@exemple.test', 'nrichard', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 541 DAY),
    ('Petit', 'Chloé', 'chloe.petit@exemple.test', 'cpetit', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 661 DAY),
    ('Durand', 'Louis', 'louis.durand@exemple.test', 'ldurand', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 518 DAY),
    ('Leroy', 'Manon', 'manon.leroy@exemple.test', 'mleroy', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 93 DAY),
    ('Moreau', 'Gabriel', 'gabriel.moreau@exemple.test', 'gmoreau', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 640 DAY),
    ('Simon', 'Inès', 'ines.simon@exemple.test', 'isimon', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 272 DAY),
    ('Laurent', 'Arthur', 'arthur.laurent@exemple.test', 'alaurent', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 701 DAY),
    ('Lefèvre', 'Jade', 'jade.lefevre@exemple.test', 'jlefevre', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 672 DAY),
    ('Michel', 'Raphaël', 'raphael.michel@exemple.test', 'rmichel', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 636 DAY),
    ('Garcia', 'Sarah', 'sarah.garcia@exemple.test', 'sgarcia', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 583 DAY),
    ('David', 'Jules', 'jules.david@exemple.test', 'jdavid', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 23 DAY),
    ('Bertrand', 'Louise', 'louise.bertrand@exemple.test', 'lbertrand', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 460 DAY),
    ('Roux', 'Adam', 'adam.roux@exemple.test', 'aroux', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 615 DAY),
    ('Vincent', 'Alice', 'alice.vincent@exemple.test', 'avincent', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 760 DAY),
    ('Fournier', 'Noah', 'noah.fournier@exemple.test', 'nfournier', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 606 DAY),
    ('Morel', 'Lina', 'lina.morel@exemple.test', 'lmorel', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 60 DAY),
    ('Girard', 'Ethan', 'ethan.girard@exemple.test', 'egirard', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 4 DAY),
    ('André', 'Zoé', 'zoe.andre@exemple.test', 'zandre', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 671 DAY),
    ('Mercier', 'Tom', 'tom.mercier@exemple.test', 'tmercier', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 771 DAY),
    ('Dupont', 'Juliette', 'juliette.dupont@exemple.test', 'jdupont', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 0 DAY),
    ('Lambert', 'Paul', 'paul.lambert@exemple.test', 'plambert', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 3 DAY),
    ('Bonnet', 'Rose', 'rose.bonnet@exemple.test', 'rbonnet', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 1 DAY),
    ('François', 'Maël', 'mael.francois@exemple.test', 'mfrancois', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', FALSE, NOW() - INTERVAL 839 DAY),
    ('Martinez', 'Anna', 'anna.martinez@exemple.test', 'amartinez', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', FALSE, NOW() - INTERVAL 659 DAY),
    ('Benali', 'Yanis', 'yanis.benali@exemple.test', 'ybenali', '$2a$10$dY4/HgDLEQ79PZ.8LnQMjuQOi5FwQnjrGFR2bNAbibWn3el3pOK/C', 'CLIENT', TRUE, NOW() - INTERVAL 534 DAY);

INSERT INTO client (id_utilisateur, telephone, adresse, date_naissance)
SELECT u.id, v.telephone, v.adresse, v.naissance
FROM (VALUES
    ROW('cmartin', '06 38 86 89 81', '54 rue du Port, 59000 Lille', DATE '2003-12-25'),
    ROW('lbernard', '07 85 66 40 10', '79 avenue Jean Jaurès, 95300 Pontoise', DATE '1968-02-15'),
    ROW('edubois', '06 97 72 96 50', '27 allée des Tilleuls, 92100 Boulogne-Billancourt', DATE '1972-06-26'),
    ROW('hthomas', '07 75 91 19 53', '12 chemin des Vignes, 59000 Lille', DATE '1968-05-15'),
    ROW('lrobert', '06 92 83 49 13', '109 rue des Écoles, 69003 Lyon', DATE '1979-07-03'),
    ROW('nrichard', '07 84 80 73 24', '55 chemin des Vignes, 95520 Osny', DATE '1998-10-16'),
    ROW('cpetit', '07 77 43 63 82', '119 rue Pasteur, 59000 Lille', DATE '1983-01-19'),
    ROW('ldurand', '06 26 16 76 95', '14 rue du Port, 13001 Marseille', DATE '1980-03-16'),
    ROW('mleroy', '06 25 90 99 68', '87 rue Pasteur, 95800 Cergy', DATE '1984-05-18'),
    ROW('gmoreau', '07 95 49 80 92', '65 rue Pasteur, 95520 Osny', DATE '1975-07-07'),
    ROW('isimon', '07 44 11 14 29', '84 rue du Port, 33000 Bordeaux', DATE '2001-09-16'),
    ROW('alaurent', '07 35 43 98 72', '60 chemin des Vignes, 95520 Osny', DATE '2002-08-26'),
    ROW('jlefevre', '07 81 94 14 47', '41 rue Pasteur, 95520 Osny', DATE '1956-08-23'),
    ROW('rmichel', '07 67 33 64 71', '9 place du Marché, 33000 Bordeaux', DATE '1973-02-07'),
    ROW('sgarcia', '07 28 67 11 21', '1 rue Pasteur, 31000 Toulouse', DATE '1984-02-20'),
    ROW('jdavid', '06 41 25 89 15', '97 chemin des Vignes, 92100 Boulogne-Billancourt', DATE '1992-11-12'),
    ROW('lbertrand', '06 10 50 95 84', '74 rue Pasteur, 95800 Cergy', DATE '1996-03-18'),
    ROW('aroux', '06 70 85 26 86', '113 chemin des Vignes, 92100 Boulogne-Billancourt', DATE '1955-07-22'),
    ROW('avincent', '06 27 85 49 74', '53 allée des Tilleuls, 95520 Osny', DATE '1959-09-26'),
    ROW('nfournier', '06 58 11 23 59', '114 avenue Jean Jaurès, 95000 Cergy', DATE '2003-06-21'),
    ROW('lmorel', '07 13 13 75 70', '42 place du Marché, 33000 Bordeaux', DATE '1970-02-16'),
    ROW('egirard', '07 83 89 31 29', '76 chemin des Vignes, 78000 Versailles', DATE '1977-10-11'),
    ROW('zandre', '07 86 39 96 18', '71 allée des Tilleuls, 95000 Cergy', DATE '1962-11-06'),
    ROW('tmercier', '06 92 79 16 67', '112 rue Victor Hugo, 33000 Bordeaux', DATE '1995-11-01'),
    ROW('jdupont', '06 92 77 37 76', '115 boulevard de l''Oise, 75011 Paris', DATE '1992-03-13'),
    ROW('plambert', '07 64 78 63 20', '50 place du Marché, 75011 Paris', DATE '1972-03-09'),
    ROW('rbonnet', '07 73 63 46 69', '23 rue des Écoles, 59000 Lille', DATE '1980-08-04'),
    ROW('mfrancois', '07 22 37 39 10', '12 rue Pasteur, 44000 Nantes', DATE '1971-07-18'),
    ROW('amartinez', '06 13 55 30 55', '72 rue du Port, 95520 Osny', DATE '1984-04-04'),
    ROW('ybenali', '07 81 82 37 82', '38 rue du Port, 92100 Boulogne-Billancourt', DATE '1992-09-05')
) AS v (identifiant, telephone, adresse, naissance)
JOIN utilisateur u ON u.identifiant = v.identifiant
ORDER BY u.id;

-- ------------------------------------------------------------
-- Contrats : une ligne par contrat dans une table de travail, supprimée à la fin.
--   debut       : jours avant aujourd'hui (négatif : commence dans le futur) ; NULL pour un renouvellement,
--                 qui commence à la fin du contrat précédent
--   paye        : payé tant de jours avant sa date de début (NULL : pas payé)
--   resiliation : résilié tant de jours avant aujourd'hui ; suspension : suspendu par un agent
--   garanties   : garanties souscrites (NULL : toutes celles du type)
-- ------------------------------------------------------------
CREATE TABLE tmp_contrat (
    id                  BIGINT UNSIGNED PRIMARY KEY,
    identifiant         VARCHAR(100) NOT NULL,
    type_assurance      VARCHAR(100) NOT NULL,
    debut               INT NULL,
    precedent           BIGINT UNSIGNED NULL,
    prime               DECIMAL(12,2) NOT NULL,
    paye                INT NULL,
    mode_paiement       VARCHAR(20) NULL,
    resiliation         INT NULL,
    motif_resiliation   VARCHAR(255) NULL,
    agent_resiliation   VARCHAR(100) NULL,
    suspension          INT NULL,
    motif_suspension    VARCHAR(255) NULL,
    garanties           VARCHAR(255) NULL,
    date_debut          DATE NULL
);

INSERT INTO tmp_contrat (id, identifiant, type_assurance, debut, precedent, prime, paye, mode_paiement,
                         resiliation, motif_resiliation, agent_resiliation, suspension, motif_suspension, garanties) VALUES
    (100002, 'client', 'Assurance habitation', 480, NULL, 260.00, 7, 'CARTE', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Dégâts des eaux,Assistance'),
    (100003, 'client', 'Assurance habitation', NULL, 100002, 260.00, 10, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Dégâts des eaux,Assistance'),
    (100004, 'client', 'Assurance voyage', -20, NULL, 85.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Assistance'),
    (100005, 'cmartin', 'Assurance automobile', -35, NULL, 635.00, 45, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Accident'),
    (100006, 'cmartin', 'Assurance automobile', 511, NULL, 665.00, 13, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Incendie,Accident,Assistance'),
    (100007, 'cmartin', 'Assurance automobile', -39, NULL, 640.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Accident,Assistance'),
    (100008, 'lbernard', 'Assurance automobile', 726, NULL, 595.00, 3, 'CHEQUE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100009, 'lbernard', 'Assurance professionnelle', 154, NULL, 885.00, 11, 'CARTE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100010, 'edubois', 'Assurance habitation', 19, NULL, 280.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    (100011, 'edubois', 'Assurance habitation', 180, NULL, 315.00, 6, 'CARTE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100012, 'hthomas', 'Assurance automobile', 272, NULL, 575.00, 11, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100013, 'lrobert', 'Assurance habitation', 576, NULL, 270.00, 12, 'CARTE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100014, 'nrichard', 'Assurance automobile', 282, NULL, 525.00, 17, 'CARTE', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Accident,Assistance'),
    (100015, 'nrichard', 'Assurance automobile', 513, NULL, 665.00, 8, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Accident,Assistance'),
    (100016, 'nrichard', 'Assurance automobile', NULL, 100015, 665.00, 1, 'CHEQUE', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Accident,Assistance'),
    (100017, 'nrichard', 'Assurance voyage', 269, NULL, 100.00, 8, 'CARTE', NULL, NULL, NULL, 90, 'Déclaration inexacte lors de la souscription', NULL),
    (100018, 'cpetit', 'Assurance automobile', 70, NULL, 640.00, NULL, NULL, 8, 'Déménagement à l''étranger', 'lpasteur', NULL, NULL, NULL),
    (100019, 'cpetit', 'Assurance professionnelle', 564, NULL, 1135.00, 7, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Incendie,Accident'),
    (100020, 'cpetit', 'Assurance habitation', 622, NULL, 280.00, 18, 'CARTE', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Dégâts des eaux'),
    (100021, 'ldurand', 'Assurance voyage', 458, NULL, 90.00, 1, 'CARTE', 430, 'Déménagement à l''étranger', 'agent', NULL, NULL, NULL),
    (100022, 'ldurand', 'Assurance santé', 14, NULL, 640.00, 17, 'CHEQUE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100023, 'ldurand', 'Assurance habitation', 501, NULL, 310.00, 12, 'CHEQUE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100024, 'ldurand', 'Assurance habitation', NULL, 100023, 310.00, 21, 'CARTE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100025, 'ldurand', 'Assurance professionnelle', 77, NULL, 1020.00, 12, 'CARTE', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Dégâts des eaux,Accident'),
    (100026, 'mleroy', 'Assurance habitation', 73, NULL, 265.00, 12, 'CARTE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100027, 'mleroy', 'Assurance voyage', -30, NULL, 85.00, 39, 'CARTE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100028, 'mleroy', 'Assurance habitation', 19, NULL, 310.00, 3, 'CHEQUE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100029, 'gmoreau', 'Assurance voyage', 159, NULL, 100.00, 6, 'CARTE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100030, 'gmoreau', 'Assurance automobile', 72, NULL, 490.00, NULL, NULL, 14, 'Vente du véhicule', 'gsand', NULL, NULL, 'Responsabilité civile,Vol,Incendie,Assistance'),
    (100031, 'gmoreau', 'Assurance habitation', 584, NULL, 270.00, 16, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100032, 'gmoreau', 'Assurance habitation', NULL, 100031, 270.00, 18, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100033, 'isimon', 'Assurance voyage', 258, NULL, 90.00, 9, 'CARTE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100034, 'alaurent', 'Assurance habitation', 341, NULL, 295.00, 15, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Assistance'),
    (100035, 'alaurent', 'Assurance santé', 682, NULL, 640.00, 12, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100036, 'jlefevre', 'Assurance santé', 142, NULL, 845.00, 10, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100037, 'jlefevre', 'Assurance automobile', 309, NULL, 670.00, 4, 'CARTE', 280, 'Changement d''assureur', 'mcurie', NULL, NULL, NULL),
    (100038, 'jlefevre', 'Assurance automobile', 624, NULL, 525.00, 12, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100039, 'jlefevre', 'Assurance automobile', NULL, 100038, 525.00, 21, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100040, 'rmichel', 'Assurance santé', 599, NULL, 875.00, 6, 'CHEQUE', 461, 'Changement d''assureur', NULL, NULL, NULL, NULL),
    (100041, 'rmichel', 'Assurance automobile', 134, NULL, 510.00, 4, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100042, 'sgarcia', 'Assurance voyage', 534, NULL, 105.00, 10, 'VIREMENT', 185, 'Vente du véhicule', NULL, NULL, NULL, NULL),
    (100043, 'sgarcia', 'Assurance automobile', 239, NULL, 495.00, 16, 'CARTE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100044, 'sgarcia', 'Assurance habitation', 10, NULL, 260.00, 3, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100045, 'jdavid', 'Assurance santé', 14, NULL, 755.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    (100046, 'jdavid', 'Assurance santé', -38, NULL, 660.00, 39, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100047, 'lbertrand', 'Assurance habitation', 418, NULL, 330.00, 15, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Incendie'),
    (100048, 'lbertrand', 'Assurance habitation', NULL, 100047, 330.00, 8, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Incendie'),
    (100049, 'aroux', 'Assurance voyage', 255, NULL, 75.00, 4, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100050, 'aroux', 'Assurance voyage', 568, NULL, 100.00, 8, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100051, 'aroux', 'Assurance automobile', 215, NULL, 605.00, 2, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100052, 'avincent', 'Assurance voyage', 252, NULL, 80.00, 19, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100053, 'avincent', 'Assurance santé', 718, NULL, 690.00, 10, 'CARTE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100054, 'avincent', 'Assurance professionnelle', 218, NULL, 1315.00, 8, 'CARTE', 136, 'Doublon avec le contrat d''un conjoint', NULL, NULL, NULL, NULL),
    (100055, 'nfournier', 'Assurance automobile', 569, NULL, 555.00, 4, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100056, 'nfournier', 'Assurance automobile', NULL, 100055, 555.00, 24, 'CHEQUE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100057, 'nfournier', 'Assurance habitation', 466, NULL, 375.00, 11, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100058, 'nfournier', 'Assurance habitation', NULL, 100057, 375.00, 23, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100059, 'lmorel', 'Assurance santé', 23, NULL, 810.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    (100060, 'egirard', 'Assurance voyage', -6, NULL, 115.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Accident'),
    (100061, 'zandre', 'Assurance habitation', 614, NULL, 265.00, 5, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Dégâts des eaux,Assistance'),
    (100062, 'zandre', 'Assurance santé', 659, NULL, 670.00, 0, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, NULL),
    (100063, 'tmercier', 'Assurance habitation', 588, NULL, 360.00, 4, 'CARTE', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Incendie,Dégâts des eaux'),
    (100064, 'tmercier', 'Assurance automobile', 740, NULL, 510.00, 8, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Incendie,Accident,Assistance'),
    (100065, 'jdupont', 'Assurance automobile', -10, NULL, 485.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    (100066, 'plambert', 'Assurance habitation', -22, NULL, 375.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    (100067, 'rbonnet', 'Assurance voyage', -27, NULL, 85.00, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol'),
    (100068, 'mfrancois', 'Assurance professionnelle', 798, NULL, 910.00, 4, 'CARTE', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Incendie,Dégâts des eaux,Accident'),
    (100069, 'amartinez', 'Assurance habitation', 639, NULL, 315.00, 1, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Incendie'),
    (100070, 'ybenali', 'Assurance habitation', 519, NULL, 305.00, 11, 'VIREMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Dégâts des eaux,Assistance'),
    (100071, 'ybenali', 'Assurance habitation', -40, NULL, 280.00, 49, 'PRELEVEMENT', NULL, NULL, NULL, NULL, NULL, 'Responsabilité civile,Vol,Incendie,Dégâts des eaux');

-- Dates de début : relatives à aujourd'hui, ou fin du contrat précédent pour un renouvellement
UPDATE tmp_contrat SET date_debut = CURDATE() - INTERVAL debut DAY WHERE precedent IS NULL;
UPDATE tmp_contrat n JOIN tmp_contrat a ON a.id = n.precedent
SET n.date_debut = a.date_debut + INTERVAL 12 MONTH;

-- Statut calculé avec la même règle que ContratService.statutAttendu
INSERT INTO contrat (id, id_client, id_type_assurance, date_debut, date_fin, prime, statut,
                     date_resiliation, motif_resiliation, date_suspension, motif_suspension, id_contrat_precedent)
SELECT t.id, c.id, ta.id, t.date_debut, t.date_debut + INTERVAL 12 MONTH, t.prime,
       CASE
           WHEN t.resiliation IS NOT NULL THEN 'RESILIE'
           WHEN CURDATE() >= t.date_debut + INTERVAL 12 MONTH THEN 'EXPIRE'
           WHEN t.suspension IS NOT NULL THEN 'SUSPENDU'
           WHEN CURDATE() < t.date_debut THEN 'EN_ATTENTE'
           WHEN t.paye IS NOT NULL THEN 'ACTIF'
           WHEN CURDATE() > t.date_debut THEN 'SUSPENDU'
           ELSE 'EN_ATTENTE'
       END,
       CURDATE() - INTERVAL t.resiliation DAY, t.motif_resiliation,
       CURDATE() - INTERVAL t.suspension DAY, t.motif_suspension, t.precedent
FROM tmp_contrat t
JOIN utilisateur u ON u.identifiant = t.identifiant
JOIN client c ON c.id_utilisateur = u.id
JOIN type_assurance ta ON ta.nom = t.type_assurance
ORDER BY t.id;

INSERT INTO contrat_garantie (id_contrat, id_garantie)
SELECT t.id, g.id
FROM tmp_contrat t
JOIN type_assurance ta ON ta.nom = t.type_assurance
JOIN type_assurance_garantie tg ON tg.id_type_assurance = ta.id
JOIN garantie g ON g.id = tg.id_garantie
WHERE t.garanties IS NULL OR FIND_IN_SET(g.nom, t.garanties);

-- Un paiement par contrat (la prime entière, due à la date de début) :
-- payé -> EFFECTUE ; pas payé -> ANNULE si résilié, EN_RETARD si l'échéance est passée, sinon EN_ATTENTE
INSERT INTO paiement (id_contrat, montant, date_echeance, date_paiement, mode_paiement, statut)
SELECT t.id, t.prime, t.date_debut, t.date_debut - INTERVAL t.paye DAY, t.mode_paiement,
       CASE
           WHEN t.paye IS NOT NULL THEN 'EFFECTUE'
           WHEN t.resiliation IS NOT NULL THEN 'ANNULE'
           WHEN t.date_debut < CURDATE() THEN 'EN_RETARD'
           ELSE 'EN_ATTENTE'
       END
FROM tmp_contrat t
ORDER BY t.id;

-- Remboursement des contrats payés puis résiliés, calculé comme ContratService.calculerRemboursement :
-- toute la prime si résilié avant le début, sinon prime x jours non couverts / jours du contrat
INSERT INTO versement (type, montant, date_versement, id_contrat, id_agent)
SELECT 'REMBOURSEMENT', r.montant, r.date_resiliation, r.id, r.id_agent
FROM (
    SELECT c.id, c.date_resiliation, a.id AS id_agent,
           CASE
               WHEN c.date_resiliation < c.date_debut THEN c.prime
               ELSE TRUNCATE(c.prime * (DATEDIFF(c.date_fin, c.date_resiliation) - 1)
                             / DATEDIFF(c.date_fin, c.date_debut), 2)
           END AS montant
    FROM contrat c
    JOIN tmp_contrat t ON t.id = c.id
    LEFT JOIN utilisateur a ON a.identifiant = t.agent_resiliation
    WHERE c.statut = 'RESILIE' AND t.paye IS NOT NULL
) r
WHERE r.montant > 0;

DROP TABLE tmp_contrat;

-- ------------------------------------------------------------
-- Sinistres, même principe (jours avant aujourd'hui) :
--   jours : date du sinistre ; declaration, prise_en_charge, decision : étapes du traitement
--   garantie et dommage : indemnisation = MIN(dommage - franchise, plafond), comme SinistreService.calculer
-- ------------------------------------------------------------
CREATE TABLE tmp_sinistre (
    id                  BIGINT UNSIGNED PRIMARY KEY,
    id_contrat          BIGINT UNSIGNED NOT NULL,
    jours               INT NOT NULL,
    declaration         INT NOT NULL,
    heure               TIME NOT NULL,
    type_sinistre       VARCHAR(20) NOT NULL,
    description         TEXT NOT NULL,
    montant_estime      DECIMAL(12,2) NULL,
    statut              VARCHAR(20) NOT NULL,
    agent               VARCHAR(100) NULL,
    prise_en_charge     INT NULL,
    decision            INT NULL,
    garantie            VARCHAR(100) NULL,
    dommage             DECIMAL(12,2) NULL,
    versement           INT NULL
);

INSERT INTO tmp_sinistre (id, id_contrat, jours, declaration, heure, type_sinistre, description, montant_estime,
                          statut, agent, prise_en_charge, decision, garantie, dommage, versement) VALUES
    (100001, 100002, 400, 400, '8:46:00', 'DEGATS_DES_EAUX', 'Fuite d''une canalisation sous l''évier.', 6070.00, 'INDEMNISE', 'vhugo', 399, 395, 'Dégâts des eaux', 5970.00, 391),
    (100002, 100001, 25, 24, '11:15:00', 'ACCIDENT', 'Accrochage sur un parking, aile avant enfoncée.', 2640.00, 'EN_ANALYSE', 'lpasteur', 23, NULL, NULL, NULL, NULL),
    (100003, 100003, 6, 3, '11:13:00', 'VOL', 'Effraction des locaux, matériel informatique volé.', 5440.00, 'DECLARE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100004, 100009, 44, 42, '8:57:00', 'VOL', 'Vol du véhicule stationné devant le domicile.', 1760.00, 'INDEMNISE', 'gsand', 38, 28, 'Vol', 1080.00, 20),
    (100005, 100044, 1, 1, '8:17:00', 'DEGATS_DES_EAUX', 'Infiltration par la toiture après un orage.', 4010.00, 'EN_ANALYSE', 'mcurie', 1, NULL, NULL, NULL, NULL),
    (100006, 100040, 581, 581, '9:31:00', 'MALADIE', 'Hospitalisation de trois jours pour une appendicite.', 3330.00, 'ACCEPTE', 'agent', 577, 572, NULL, NULL, NULL),
    (100007, 100016, 23, 23, '9:10:00', 'ACCIDENT', 'Collision avec un autre véhicule à un carrefour.', 2560.00, 'INDEMNISE', 'mcurie', 22, 14, 'Accident', 1740.00, 8),
    (100008, 100028, 6, 6, '9:52:00', 'INCENDIE', 'Départ de feu dans la cuisine.', NULL, 'DECLARE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100009, 100024, 39, 38, '8:44:00', 'VOL', 'Vol du véhicule stationné devant le domicile.', 1300.00, 'ACCEPTE', 'gsand', 35, 25, NULL, NULL, NULL),
    (100010, 100069, 319, 314, '9:19:00', 'DEGATS_DES_EAUX', 'Fuite d''une canalisation sous l''évier.', NULL, 'INDEMNISE', 'lpasteur', 314, 314, 'Responsabilité civile', 2630.00, 306),
    (100011, 100031, 463, 459, '9:00:00', 'DEGATS_DES_EAUX', 'Infiltration par la toiture après un orage.', 7480.00, 'ACCEPTE', 'lpasteur', 455, 451, NULL, NULL, NULL),
    (100012, 100026, 57, 56, '8:14:00', 'VOL', 'Effraction des locaux, matériel informatique volé.', 5740.00, 'ACCEPTE', 'vhugo', 53, 41, NULL, NULL, NULL),
    (100013, 100038, 486, 481, '11:30:00', 'INCENDIE', 'Incendie d''origine électrique dans le garage.', 12950.00, 'ACCEPTE', 'vhugo', 479, 477, NULL, NULL, NULL),
    (100014, 100050, 310, 305, '10:10:00', 'VOL', 'Effraction des locaux, matériel informatique volé.', 8810.00, 'INDEMNISE', 'agent', 304, 298, 'Vol', 5740.00, 292),
    (100015, 100008, 679, 677, '10:55:00', 'AUTRE', 'Perte des clés, intervention d''un serrurier.', 540.00, 'ACCEPTE', 'gsand', 676, 674, NULL, NULL, NULL),
    (100016, 100033, 101, 100, '11:40:00', 'ACCIDENT', 'Chute dans un escalier, entorse de la cheville.', 4150.00, 'REFUSE', 'gsand', 100, 91, NULL, NULL, NULL),
    (100017, 100051, 144, 138, '10:53:00', 'VOL', 'Vol d''un sac contenant téléphone et papiers.', 8860.00, 'INDEMNISE', 'vhugo', 135, 127, 'Vol', 6100.00, 120),
    (100018, 100061, 438, 434, '8:06:00', 'DEGATS_DES_EAUX', 'Débordement de la machine à laver chez le voisin du dessus.', NULL, 'INDEMNISE', 'mcurie', 433, 431, 'Dégâts des eaux', 3090.00, 428),
    (100019, 100019, 217, 217, '8:26:00', 'INCENDIE', 'Incendie d''origine électrique dans le garage.', 16810.00, 'INDEMNISE', 'mcurie', 215, 211, 'Incendie', 16120.00, 209),
    (100020, 100052, 196, 191, '9:55:00', 'ACCIDENT', 'Collision avec un autre véhicule à un carrefour.', 3910.00, 'INDEMNISE', 'lpasteur', 188, 183, 'Accident', 3280.00, 181),
    (100021, 100015, 378, 372, '11:48:00', 'INCENDIE', 'Feu de poubelle propagé à la façade.', 36850.00, 'ACCEPTE', 'mcurie', 372, 372, NULL, NULL, NULL),
    (100022, 100034, 227, 227, '11:11:00', 'INCENDIE', 'Départ de feu dans la cuisine.', NULL, 'INDEMNISE', 'gsand', 227, 223, 'Responsabilité civile', 23440.00, 216),
    (100023, 100022, 3, 1, '9:23:00', 'ACCIDENT', 'Accident de vélo sur le trajet du travail.', 1850.00, 'EN_ANALYSE', 'agent', 1, NULL, NULL, NULL, NULL),
    (100024, 100012, 43, 37, '10:14:00', 'AUTRE', 'Bris de glace sur le pare-brise.', NULL, 'ACCEPTE', 'lpasteur', 33, 31, NULL, NULL, NULL),
    (100025, 100055, 491, 489, '10:13:00', 'AUTRE', 'Bris de glace sur le pare-brise.', NULL, 'INDEMNISE', 'lpasteur', 486, 484, 'Assistance', 320.00, 479),
    (100026, 100023, 139, 135, '8:42:00', 'DEGATS_DES_EAUX', 'Débordement de la machine à laver chez le voisin du dessus.', NULL, 'INDEMNISE', 'vhugo', 132, 122, 'Dégâts des eaux', 2800.00, 115),
    (100027, 100021, 454, 451, '10:24:00', 'MALADIE', 'Consultation et soins d''urgence pendant un séjour.', 2650.00, 'INDEMNISE', 'vhugo', 449, 437, 'Assistance', 2100.00, 431),
    (100028, 100036, 6, 4, '10:27:00', 'ACCIDENT', 'Collision avec un autre véhicule à un carrefour.', 3450.00, 'EN_ANALYSE', 'vhugo', 1, NULL, NULL, NULL, NULL),
    (100029, 100011, 1, 1, '11:06:00', 'DEGATS_DES_EAUX', 'Fuite d''une canalisation sous l''évier.', 2060.00, 'DECLARE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100030, 100039, 28, 27, '8:58:00', 'VOL', 'Vol du véhicule stationné devant le domicile.', 3940.00, 'INDEMNISE', 'agent', 27, 18, 'Vol', 3740.00, 11),
    (100031, 100056, 189, 183, '11:12:00', 'ACCIDENT', 'Accrochage sur un parking, aile avant enfoncée.', 2010.00, 'REFUSE', 'agent', 180, 176, NULL, NULL, NULL),
    (100032, 100049, 156, 151, '11:40:00', 'ACCIDENT', 'Accident de vélo sur le trajet du travail.', 2940.00, 'INDEMNISE', 'mcurie', 151, 147, 'Accident', 2680.00, 145),
    (100033, 100068, 574, 570, '10:01:00', 'ACCIDENT', 'Chute dans un escalier, entorse de la cheville.', 1550.00, 'INDEMNISE', 'agent', 569, 562, 'Accident', 1170.00, 556),
    (100034, 100006, 336, 331, '8:19:00', 'AUTRE', 'Panne sur l''autoroute, remorquage nécessaire.', 550.00, 'INDEMNISE', 'lpasteur', 327, 326, 'Assistance', 510.00, 324),
    (100035, 100048, 23, 22, '10:37:00', 'DEGATS_DES_EAUX', 'Débordement de la machine à laver chez le voisin du dessus.', 4550.00, 'INDEMNISE', 'gsand', 20, 17, 'Responsabilité civile', 3070.00, 14),
    (100036, 100014, 243, 238, '8:35:00', 'ACCIDENT', 'Accrochage sur un parking, aile avant enfoncée.', 3890.00, 'INDEMNISE', 'vhugo', 237, 226, 'Accident', 2760.00, 225),
    (100037, 100054, 155, 152, '8:16:00', 'VOL', 'Vol du véhicule stationné devant le domicile.', 7170.00, 'INDEMNISE', 'gsand', 152, 145, 'Vol', 5210.00, 140),
    (100038, 100064, 412, 406, '8:01:00', 'VOL', 'Cambriolage : ordinateur et bijoux dérobés.', NULL, 'INDEMNISE', 'vhugo', 405, 405, 'Responsabilité civile', 4180.00, 400),
    (100039, 100058, 2, 1, '10:51:00', 'VOL', 'Vol d''un sac contenant téléphone et papiers.', 5010.00, 'DECLARE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100040, 100033, 28, 23, '8:41:00', 'VOL', 'Vol du véhicule stationné devant le domicile.', 6890.00, 'EN_ANALYSE', 'lpasteur', 23, NULL, NULL, NULL, NULL),
    (100041, 100049, 13, 8, '9:27:00', 'VOL', 'Vol du véhicule stationné devant le domicile.', 7800.00, 'ACCEPTE', 'mcurie', 7, 3, NULL, NULL, NULL),
    (100042, 100026, 15, 15, '11:57:00', 'DEGATS_DES_EAUX', 'Infiltration par la toiture après un orage.', 7780.00, 'DECLARE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100043, 100011, 21, 15, '11:18:00', 'DEGATS_DES_EAUX', 'Fuite d''une canalisation sous l''évier.', 4940.00, 'DECLARE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100044, 100034, 14, 13, '11:16:00', 'INCENDIE', 'Incendie d''origine électrique dans le garage.', NULL, 'DECLARE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100045, 100043, 23, 18, '9:47:00', 'ACCIDENT', 'Accrochage sur un parking, aile avant enfoncée.', 4550.00, 'ACCEPTE', 'vhugo', 17, 16, NULL, NULL, NULL),
    (100046, 100041, 25, 23, '10:54:00', 'ACCIDENT', 'Accident de vélo sur le trajet du travail.', 3010.00, 'ACCEPTE', 'vhugo', 23, 13, NULL, NULL, NULL),
    (100047, 100056, 23, 17, '11:05:00', 'ACCIDENT', 'Accident de vélo sur le trajet du travail.', 3340.00, 'DECLARE', NULL, NULL, NULL, NULL, NULL, NULL),
    (100048, 100051, 22, 22, '10:51:00', 'VOL', 'Cambriolage : ordinateur et bijoux dérobés.', 4840.00, 'EN_ANALYSE', 'lpasteur', 20, NULL, NULL, NULL, NULL),
    (100049, 100048, 9, 9, '10:29:00', 'VOL', 'Vol d''un sac contenant téléphone et papiers.', 8220.00, 'EN_ANALYSE', 'lpasteur', 9, NULL, NULL, NULL, NULL),
    (100050, 100025, 26, 23, '11:48:00', 'VOL', 'Vol du véhicule stationné devant le domicile.', 3700.00, 'INDEMNISE', 'lpasteur', 23, 12, 'Vol', 3050.00, 10);

INSERT INTO sinistre (id, id_contrat, date_sinistre, date_declaration, description, type_sinistre, montant_estime,
                      statut, date_prise_en_charge, date_decision, id_agent)
SELECT s.id, s.id_contrat, CURDATE() - INTERVAL s.jours DAY,
       TIMESTAMP(CURDATE() - INTERVAL s.declaration DAY, s.heure),
       s.description, s.type_sinistre, s.montant_estime, s.statut,
       TIMESTAMP(CURDATE() - INTERVAL s.prise_en_charge DAY, '14:00:00'),
       TIMESTAMP(CURDATE() - INTERVAL s.decision DAY, '15:45:00'),
       a.id
FROM tmp_sinistre s
LEFT JOIN utilisateur a ON a.identifiant = s.agent
ORDER BY s.id;

INSERT INTO versement (type, montant, date_versement, id_contrat, id_sinistre, id_agent, id_garantie,
                       montant_dommage, franchise_appliquee, plafond_applique)
SELECT 'INDEMNISATION', LEAST(s.dommage - COALESCE(g.franchise, 0), COALESCE(g.plafond, s.dommage)),
       CURDATE() - INTERVAL s.versement DAY, s.id_contrat, s.id, a.id, g.id,
       s.dommage, COALESCE(g.franchise, 0), g.plafond
FROM tmp_sinistre s
JOIN garantie g ON g.nom = s.garantie
JOIN utilisateur a ON a.identifiant = s.agent
WHERE s.statut = 'INDEMNISE'
ORDER BY s.id;

DROP TABLE tmp_sinistre;

-- Discussions sur les sinistres (auteur : le client du contrat ou un agent)
INSERT INTO commentaire_sinistre (id_sinistre, id_auteur, contenu, date_commentaire)
SELECT v.id_sinistre, u.id, v.contenu, TIMESTAMP(CURDATE() - INTERVAL v.jours DAY, v.heure)
FROM (VALUES
    ROW(100001, 'vhugo', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 399, '14:30:00'),
    ROW(100001, 'client', 'Voici la facture, merci de votre réactivité.', 397, '15:00:00'),
    ROW(100001, 'vhugo', 'Votre dossier est accepté, l''indemnisation va être calculée.', 395, '16:10:00'),
    ROW(100002, 'lpasteur', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 23, '14:30:00'),
    ROW(100002, 'client', 'Bonjour, je viens d''ajouter les photos et le devis du réparateur.', 21, '15:00:00'),
    ROW(100003, 'client', 'Bonjour, avez-vous bien reçu ma déclaration ?', 2, '18:00:00'),
    ROW(100004, 'gsand', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 38, '14:30:00'),
    ROW(100004, 'gsand', 'Votre dossier est accepté, l''indemnisation va être calculée.', 28, '16:10:00'),
    ROW(100005, 'mcurie', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 1, '14:30:00'),
    ROW(100005, 'sgarcia', 'Je vous envoie les justificatifs dès que possible.', 1, '15:00:00'),
    ROW(100006, 'agent', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 577, '14:30:00'),
    ROW(100006, 'rmichel', 'Voici la facture, merci de votre réactivité.', 576, '15:00:00'),
    ROW(100006, 'agent', 'Votre dossier est accepté, l''indemnisation va être calculée.', 572, '16:10:00'),
    ROW(100007, 'mcurie', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 22, '14:30:00'),
    ROW(100007, 'nrichard', 'Je vous envoie les justificatifs dès que possible.', 20, '15:00:00'),
    ROW(100007, 'mcurie', 'Votre dossier est accepté, l''indemnisation va être calculée.', 14, '16:10:00'),
    ROW(100009, 'gsand', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 35, '14:30:00'),
    ROW(100009, 'ldurand', 'Je vous envoie les justificatifs dès que possible.', 34, '15:00:00'),
    ROW(100009, 'gsand', 'Votre dossier est accepté, l''indemnisation va être calculée.', 25, '16:10:00'),
    ROW(100010, 'lpasteur', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 314, '14:30:00'),
    ROW(100010, 'amartinez', 'Bonjour, je viens d''ajouter les photos et le devis du réparateur.', 314, '15:00:00'),
    ROW(100010, 'lpasteur', 'Votre dossier est accepté, l''indemnisation va être calculée.', 314, '16:10:00'),
    ROW(100011, 'lpasteur', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 455, '14:30:00'),
    ROW(100011, 'gmoreau', 'Voici la facture, merci de votre réactivité.', 455, '15:00:00'),
    ROW(100011, 'lpasteur', 'Votre dossier est accepté, l''indemnisation va être calculée.', 451, '16:10:00'),
    ROW(100012, 'vhugo', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 53, '14:30:00'),
    ROW(100012, 'mleroy', 'Voici la facture, merci de votre réactivité.', 52, '15:00:00'),
    ROW(100012, 'vhugo', 'Votre dossier est accepté, l''indemnisation va être calculée.', 41, '16:10:00'),
    ROW(100013, 'vhugo', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 479, '14:30:00'),
    ROW(100013, 'jlefevre', 'Je vous envoie les justificatifs dès que possible.', 479, '15:00:00'),
    ROW(100013, 'vhugo', 'Votre dossier est accepté, l''indemnisation va être calculée.', 477, '16:10:00'),
    ROW(100014, 'agent', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 304, '14:30:00'),
    ROW(100014, 'aroux', 'Voici la facture, merci de votre réactivité.', 304, '15:00:00'),
    ROW(100014, 'agent', 'Votre dossier est accepté, l''indemnisation va être calculée.', 298, '16:10:00'),
    ROW(100015, 'gsand', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 676, '14:30:00'),
    ROW(100015, 'lbernard', 'Bonjour, je viens d''ajouter les photos et le devis du réparateur.', 674, '15:00:00'),
    ROW(100015, 'gsand', 'Votre dossier est accepté, l''indemnisation va être calculée.', 674, '16:10:00'),
    ROW(100016, 'gsand', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 100, '14:30:00'),
    ROW(100016, 'isimon', 'Bonjour, je viens d''ajouter les photos et le devis du réparateur.', 98, '15:00:00'),
    ROW(100016, 'gsand', 'Sinistre refusé. Motif : Aucun justificatif fourni malgré nos relances.', 91, '16:00:00'),
    ROW(100017, 'vhugo', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 135, '14:30:00'),
    ROW(100017, 'aroux', 'Voici la facture, merci de votre réactivité.', 133, '15:00:00'),
    ROW(100017, 'vhugo', 'Votre dossier est accepté, l''indemnisation va être calculée.', 127, '16:10:00'),
    ROW(100018, 'mcurie', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 433, '14:30:00'),
    ROW(100018, 'zandre', 'Bonjour, je viens d''ajouter les photos et le devis du réparateur.', 432, '15:00:00'),
    ROW(100018, 'mcurie', 'Votre dossier est accepté, l''indemnisation va être calculée.', 431, '16:10:00'),
    ROW(100019, 'mcurie', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 215, '14:30:00'),
    ROW(100019, 'mcurie', 'Votre dossier est accepté, l''indemnisation va être calculée.', 211, '16:10:00'),
    ROW(100020, 'lpasteur', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 188, '14:30:00'),
    ROW(100020, 'avincent', 'Bonjour, je viens d''ajouter les photos et le devis du réparateur.', 188, '15:00:00'),
    ROW(100020, 'lpasteur', 'Votre dossier est accepté, l''indemnisation va être calculée.', 183, '16:10:00'),
    ROW(100021, 'mcurie', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 372, '14:30:00'),
    ROW(100021, 'nrichard', 'Voici la facture, merci de votre réactivité.', 372, '15:00:00'),
    ROW(100021, 'mcurie', 'Votre dossier est accepté, l''indemnisation va être calculée.', 372, '16:10:00'),
    ROW(100022, 'gsand', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 227, '14:30:00'),
    ROW(100022, 'alaurent', 'Bonjour, je viens d''ajouter les photos et le devis du réparateur.', 227, '15:00:00'),
    ROW(100022, 'gsand', 'Votre dossier est accepté, l''indemnisation va être calculée.', 223, '16:10:00'),
    ROW(100023, 'agent', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 1, '14:30:00'),
    ROW(100024, 'lpasteur', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 33, '14:30:00'),
    ROW(100024, 'hthomas', 'Voici la facture, merci de votre réactivité.', 33, '15:00:00'),
    ROW(100024, 'lpasteur', 'Votre dossier est accepté, l''indemnisation va être calculée.', 31, '16:10:00'),
    ROW(100025, 'lpasteur', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 486, '14:30:00'),
    ROW(100025, 'nfournier', 'Je vous envoie les justificatifs dès que possible.', 486, '15:00:00'),
    ROW(100025, 'lpasteur', 'Votre dossier est accepté, l''indemnisation va être calculée.', 484, '16:10:00'),
    ROW(100026, 'vhugo', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 132, '14:30:00'),
    ROW(100026, 'ldurand', 'Voici la facture, merci de votre réactivité.', 131, '15:00:00'),
    ROW(100026, 'vhugo', 'Votre dossier est accepté, l''indemnisation va être calculée.', 122, '16:10:00'),
    ROW(100027, 'vhugo', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 449, '14:30:00'),
    ROW(100027, 'ldurand', 'Bonjour, je viens d''ajouter les photos et le devis du réparateur.', 447, '15:00:00'),
    ROW(100027, 'vhugo', 'Votre dossier est accepté, l''indemnisation va être calculée.', 437, '16:10:00'),
    ROW(100028, 'vhugo', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 1, '14:30:00'),
    ROW(100028, 'jlefevre', 'Je vous envoie les justificatifs dès que possible.', 1, '15:00:00'),
    ROW(100030, 'agent', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 27, '14:30:00'),
    ROW(100030, 'jlefevre', 'Voici la facture, merci de votre réactivité.', 25, '15:00:00'),
    ROW(100030, 'agent', 'Votre dossier est accepté, l''indemnisation va être calculée.', 18, '16:10:00'),
    ROW(100031, 'agent', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 180, '14:30:00'),
    ROW(100031, 'nfournier', 'Je vous envoie les justificatifs dès que possible.', 178, '15:00:00'),
    ROW(100031, 'agent', 'Sinistre refusé. Motif : Le montant du dommage ne dépasse pas la franchise de la garantie.', 176, '16:00:00'),
    ROW(100032, 'mcurie', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 151, '14:30:00'),
    ROW(100032, 'aroux', 'Voici la facture, merci de votre réactivité.', 151, '15:00:00'),
    ROW(100032, 'mcurie', 'Votre dossier est accepté, l''indemnisation va être calculée.', 147, '16:10:00'),
    ROW(100033, 'agent', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 569, '14:30:00'),
    ROW(100033, 'mfrancois', 'Bonjour, je viens d''ajouter les photos et le devis du réparateur.', 569, '15:00:00'),
    ROW(100033, 'agent', 'Votre dossier est accepté, l''indemnisation va être calculée.', 562, '16:10:00'),
    ROW(100034, 'lpasteur', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 327, '14:30:00'),
    ROW(100034, 'cmartin', 'Voici la facture, merci de votre réactivité.', 326, '15:00:00'),
    ROW(100034, 'lpasteur', 'Votre dossier est accepté, l''indemnisation va être calculée.', 326, '16:10:00'),
    ROW(100035, 'gsand', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 20, '14:30:00'),
    ROW(100035, 'gsand', 'Votre dossier est accepté, l''indemnisation va être calculée.', 17, '16:10:00'),
    ROW(100036, 'vhugo', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 237, '14:30:00'),
    ROW(100036, 'vhugo', 'Votre dossier est accepté, l''indemnisation va être calculée.', 226, '16:10:00'),
    ROW(100037, 'gsand', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 152, '14:30:00'),
    ROW(100037, 'avincent', 'Je vous envoie les justificatifs dès que possible.', 152, '15:00:00'),
    ROW(100037, 'gsand', 'Votre dossier est accepté, l''indemnisation va être calculée.', 145, '16:10:00'),
    ROW(100038, 'vhugo', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 405, '14:30:00'),
    ROW(100038, 'tmercier', 'Je vous envoie les justificatifs dès que possible.', 405, '15:00:00'),
    ROW(100038, 'vhugo', 'Votre dossier est accepté, l''indemnisation va être calculée.', 405, '16:10:00'),
    ROW(100039, 'nfournier', 'Bonjour, avez-vous bien reçu ma déclaration ?', 1, '18:00:00'),
    ROW(100040, 'lpasteur', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 23, '14:30:00'),
    ROW(100040, 'isimon', 'Je vous envoie les justificatifs dès que possible.', 22, '15:00:00'),
    ROW(100041, 'mcurie', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 7, '14:30:00'),
    ROW(100041, 'mcurie', 'Votre dossier est accepté, l''indemnisation va être calculée.', 3, '16:10:00'),
    ROW(100043, 'edubois', 'Bonjour, avez-vous bien reçu ma déclaration ?', 14, '18:00:00'),
    ROW(100045, 'vhugo', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 17, '14:30:00'),
    ROW(100045, 'sgarcia', 'Bonjour, je viens d''ajouter les photos et le devis du réparateur.', 16, '15:00:00'),
    ROW(100045, 'vhugo', 'Votre dossier est accepté, l''indemnisation va être calculée.', 16, '16:10:00'),
    ROW(100046, 'vhugo', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 23, '14:30:00'),
    ROW(100046, 'rmichel', 'Voici la facture, merci de votre réactivité.', 22, '15:00:00'),
    ROW(100046, 'vhugo', 'Votre dossier est accepté, l''indemnisation va être calculée.', 13, '16:10:00'),
    ROW(100047, 'nfournier', 'Bonjour, avez-vous bien reçu ma déclaration ?', 16, '18:00:00'),
    ROW(100048, 'lpasteur', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 20, '14:30:00'),
    ROW(100049, 'lpasteur', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 9, '14:30:00'),
    ROW(100049, 'lbertrand', 'Bonjour, je viens d''ajouter les photos et le devis du réparateur.', 8, '15:00:00'),
    ROW(100050, 'lpasteur', 'Bonjour, je prends en charge votre dossier. Pouvez-vous nous envoyer des photos et une facture ou un devis ?', 23, '14:30:00'),
    ROW(100050, 'ldurand', 'Bonjour, je viens d''ajouter les photos et le devis du réparateur.', 21, '15:00:00'),
    ROW(100050, 'lpasteur', 'Votre dossier est accepté, l''indemnisation va être calculée.', 12, '16:10:00')
) AS v (id_sinistre, identifiant, contenu, jours, heure)
JOIN utilisateur u ON u.identifiant = v.identifiant
ORDER BY v.id_sinistre, v.jours DESC, v.heure;
