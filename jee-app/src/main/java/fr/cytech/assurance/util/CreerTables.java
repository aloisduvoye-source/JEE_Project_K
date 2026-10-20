package fr.cytech.assurance.util;

// Classe temporaire : démarre Hibernate pour qu'il crée les tables à partir des entités
// (selon hibernate.hbm2ddl.auto dans persistence.xml). À lancer depuis l'IDE.
public class CreerTables {
    public static void main(String[] args) {
        JpaUtil.getEntityManager().close();
        JpaUtil.shutdown();
    }
}
