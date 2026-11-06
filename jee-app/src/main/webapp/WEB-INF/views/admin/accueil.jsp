<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Administration · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <section class="banniere">
        <p class="banniere-sur-titre">Administration</p>
        <h1>Bonjour <c:out value="${sessionScope.utilisateur.prenom}"/>,</h1>
        <p>Gérez le catalogue des types d'assurance et des garanties, et consultez l'ensemble des contrats. La gestion des utilisateurs et les indicateurs arrivent bientôt.</p>
        <div class="banniere-actions">
          <a class="bouton bouton-blanc" href="${pageContext.request.contextPath}/admin/types-assurance">
            <svg class="icone" aria-hidden="true"><use href="#i-couches"/></svg>Types d'assurance
          </a>
          <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/agent/contrats">Voir les contrats</a>
        </div>
      </section>

      <div>
        <div class="titre-section"><h2>Gestion</h2></div>
        <%-- Les tuiles "Bientôt" deviendront des liens quand les pages /admin/... existeront --%>
        <div class="grille grille-tuiles">
          <a class="tuile" href="${pageContext.request.contextPath}/admin/types-assurance">
            <span class="pastille ton-violet"><svg class="icone" aria-hidden="true"><use href="#i-couches"/></svg></span>
            <div>
              <h3>Types d'assurance</h3>
              <p>Créer, modifier, archiver un type et choisir ses garanties.</p>
            </div>
            <span class="tuile-lien">Gérer <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg></span>
          </a>
          <a class="tuile" href="${pageContext.request.contextPath}/admin/garanties">
            <span class="pastille ton-succes"><svg class="icone" aria-hidden="true"><use href="#i-bouclier-ok"/></svg></span>
            <div>
              <h3>Garanties</h3>
              <p>Créer, modifier, supprimer une garantie, avec son plafond et sa franchise.</p>
            </div>
            <span class="tuile-lien">Gérer <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg></span>
          </a>
          <a class="tuile" href="${pageContext.request.contextPath}/agent/contrats">
            <span class="pastille ton-primaire"><svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg></span>
            <div>
              <h3>Contrats</h3>
              <p>L'ensemble des contrats, avec leur client et leur statut.</p>
            </div>
            <span class="tuile-lien">Consulter <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg></span>
          </a>
          <div class="tuile tuile-bientot">
            <span class="badge ton-neutre">Bientôt</span>
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-utilisateurs"/></svg></span>
            <div><h3>Utilisateurs</h3><p>Créer des agents, activer ou désactiver des comptes.</p></div>
          </div>
          <div class="tuile tuile-bientot">
            <span class="badge ton-neutre">Bientôt</span>
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg></span>
            <div><h3>Sinistres</h3><p>Tous les sinistres et leur traitement.</p></div>
          </div>
        </div>
      </div>

      <div>
        <div class="titre-section">
          <h2>Indicateurs de performance</h2>
          <span class="badge ton-neutre">Bientôt</span>
        </div>
        <div class="grille grille-stats">
          <div class="carte stat">
            <div class="stat-entete"><span class="stat-libelle">Indemnisations / primes</span><span class="pastille pastille-petite ton-primaire"><svg class="icone" aria-hidden="true"><use href="#i-indicateurs"/></svg></span></div>
            <div class="stat-valeur en-attente">—</div>
            <div class="stat-detail">Ratio sur les primes encaissées</div>
          </div>
          <div class="carte stat">
            <div class="stat-entete"><span class="stat-libelle">Traitement d'un sinistre</span><span class="pastille pastille-petite ton-info"><svg class="icone" aria-hidden="true"><use href="#i-horloge"/></svg></span></div>
            <div class="stat-valeur en-attente">—</div>
            <div class="stat-detail">Délai moyen</div>
          </div>
          <div class="carte stat">
            <div class="stat-entete"><span class="stat-libelle">Sinistres acceptés</span><span class="pastille pastille-petite ton-succes"><svg class="icone" aria-hidden="true"><use href="#i-succes"/></svg></span></div>
            <div class="stat-valeur en-attente">—</div>
            <div class="stat-detail">Taux d'acceptation</div>
          </div>
          <div class="carte stat">
            <div class="stat-entete"><span class="stat-libelle">Résiliations</span><span class="pastille pastille-petite ton-danger"><svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg></span></div>
            <div class="stat-valeur en-attente">—</div>
            <div class="stat-detail">Taux de résiliation des contrats</div>
          </div>
          <div class="carte stat">
            <div class="stat-entete"><span class="stat-libelle">Paiements en retard</span><span class="pastille pastille-petite ton-attention"><svg class="icone" aria-hidden="true"><use href="#i-carte"/></svg></span></div>
            <div class="stat-valeur en-attente">—</div>
            <div class="stat-detail">Échéances dépassées</div>
          </div>
        </div>
      </div>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
