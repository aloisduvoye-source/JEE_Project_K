<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Espace agent · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <section class="banniere">
        <p class="banniere-sur-titre">Espace agent</p>
        <h1>Bonjour <c:out value="${sessionScope.utilisateur.prenom}"/>,</h1>
        <p>Créez et suivez les contrats de vos clients, traitez leurs sinistres et gérez les garanties. Les statistiques arrivent bientôt.</p>
        <div class="banniere-actions">
          <a class="bouton bouton-blanc" href="${pageContext.request.contextPath}/agent/contrats/nouveau">
            <svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg>Nouveau contrat
          </a>
          <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/agent/sinistres?statut=DECLARE">Sinistres à prendre en charge</a>
        </div>
      </section>

      <div>
        <div class="titre-section"><h2>Gestion</h2></div>
        <%-- Les tuiles "Bientôt" deviendront des liens quand les pages /agent/... existeront --%>
        <div class="grille grille-tuiles">
          <a class="tuile" href="${pageContext.request.contextPath}/agent/contrats">
            <span class="pastille ton-primaire"><svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg></span>
            <div>
              <h3>Contrats</h3>
              <p>Tous les contrats, avec leur client, leur période et leur statut.</p>
            </div>
            <span class="tuile-lien">Ouvrir <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg></span>
          </a>
          <a class="tuile" href="${pageContext.request.contextPath}/agent/contrats/nouveau">
            <span class="pastille ton-succes"><svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg></span>
            <div>
              <h3>Nouveau contrat</h3>
              <p>Choisir un type d'assurance, un client et ses garanties.</p>
            </div>
            <span class="tuile-lien">Créer <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg></span>
          </a>
          <a class="tuile" href="${pageContext.request.contextPath}/agent/clients">
            <span class="pastille ton-info"><svg class="icone" aria-hidden="true"><use href="#i-utilisateurs"/></svg></span>
            <div>
              <h3>Clients</h3>
              <p>Rechercher un client et consulter ses contrats.</p>
            </div>
            <span class="tuile-lien">Ouvrir <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg></span>
          </a>
          <a class="tuile" href="${pageContext.request.contextPath}/agent/garanties">
            <span class="pastille ton-violet"><svg class="icone" aria-hidden="true"><use href="#i-bouclier-ok"/></svg></span>
            <div>
              <h3>Garanties</h3>
              <p>Créer, modifier et supprimer les garanties proposées.</p>
            </div>
            <span class="tuile-lien">Gérer <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg></span>
          </a>
          <a class="tuile" href="${pageContext.request.contextPath}/agent/sinistres">
            <span class="pastille ton-orange"><svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg></span>
            <div>
              <h3>Sinistres</h3>
              <p>Prendre en charge, analyser, accepter ou refuser les sinistres, et indemniser.</p>
            </div>
            <span class="tuile-lien">Traiter <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg></span>
          </a>
          <a class="tuile" href="${pageContext.request.contextPath}/agent/paiements?statut=EN_RETARD">
            <span class="pastille ton-attention"><svg class="icone" aria-hidden="true"><use href="#i-carte"/></svg></span>
            <div>
              <h3>Paiements</h3>
              <p>Suivre les paiements et les retards de tous les contrats.</p>
            </div>
            <span class="tuile-lien">Voir les retards <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg></span>
          </a>
        </div>
      </div>

      <div>
        <div class="titre-section">
          <h2>Statistiques</h2>
          <span class="badge ton-neutre">Bientôt</span>
        </div>
        <div class="grille grille-stats">
          <div class="carte stat">
            <div class="stat-entete"><span class="stat-libelle">Clients</span><span class="pastille pastille-petite ton-primaire"><svg class="icone" aria-hidden="true"><use href="#i-utilisateurs"/></svg></span></div>
            <div class="stat-valeur en-attente">—</div>
            <div class="stat-detail">Total et nouveaux clients</div>
          </div>
          <div class="carte stat">
            <div class="stat-entete"><span class="stat-libelle">Contrats actifs</span><span class="pastille pastille-petite ton-succes"><svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg></span></div>
            <div class="stat-valeur en-attente">—</div>
            <div class="stat-detail">Expirés, résiliés, évolution</div>
          </div>
          <div class="carte stat">
            <div class="stat-entete"><span class="stat-libelle">Chiffre d'affaires</span><span class="pastille pastille-petite ton-info"><svg class="icone" aria-hidden="true"><use href="#i-euro"/></svg></span></div>
            <div class="stat-valeur en-attente">—</div>
            <div class="stat-detail">Paiements encaissés</div>
          </div>
          <div class="carte stat">
            <div class="stat-entete"><span class="stat-libelle">Indemnisations</span><span class="pastille pastille-petite ton-attention"><svg class="icone" aria-hidden="true"><use href="#i-stats"/></svg></span></div>
            <div class="stat-valeur en-attente">—</div>
            <div class="stat-detail">Montants versés aux clients</div>
          </div>
        </div>
      </div>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
