-- dump.sql
-- Données de la base "assurance" : jeu de données de test
-- (utilisateurs, clients, contrats, garanties, sinistres, paiements...).
-- À exécuter après schema.sql.

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
