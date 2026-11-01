package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.TypeAssuranceDao;
import fr.cytech.assurance.model.TypeAssurance;

import java.util.List;

// Règles et lectures concernant le catalogue des types d'assurance
public class TypeAssuranceService {

    private final TypeAssuranceDao typeAssuranceDao = new TypeAssuranceDao();

    // Types d'assurance triés par nom
    public List<TypeAssurance> listerTous() {
        return typeAssuranceDao.listerParNom();
    }

    // Type avec ses garanties disponibles, ou null
    public TypeAssurance trouverAvecGaranties(Long id) {
        return typeAssuranceDao.trouverAvecGaranties(id);
    }
}
