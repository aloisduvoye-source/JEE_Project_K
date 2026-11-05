<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Mon espace · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <section class="banniere">
        <p class="banniere-sur-titre">Espace client</p>
        <h1>Bonjour <c:out value="${sessionScope.utilisateur.prenom}"/>,</h1>
        <p>Retrouvez vos contrats et vos garanties. La déclaration de sinistre, les paiements et l'assistant arrivent bientôt dans votre espace.</p>
        <div class="banniere-actions">
          <a class="bouton bouton-blanc" href="${pageContext.request.contextPath}/client/contrats">
            <svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg>Voir mes contrats
          </a>
          <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/compte">Mon compte</a>
        </div>
      </section>

      <div>
        <div class="titre-section"><h2>Mes démarches</h2></div>
        <%-- Les tuiles "Bientôt" deviendront des liens quand les pages /client/... existeront --%>
        <div class="grille grille-tuiles">
          <a class="tuile" href="${pageContext.request.contextPath}/client/contrats">
            <span class="pastille ton-primaire"><svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg></span>
            <div>
              <h3>Mes contrats</h3>
              <p>Période, prime et statut de chacun de vos contrats.</p>
            </div>
            <span class="tuile-lien">Consulter <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg></span>
          </a>
          <a class="tuile" href="${pageContext.request.contextPath}/client/contrats">
            <span class="pastille ton-succes"><svg class="icone" aria-hidden="true"><use href="#i-bouclier-ok"/></svg></span>
            <div>
              <h3>Mes garanties</h3>
              <p>Plafonds et franchises des garanties de vos contrats.</p>
            </div>
            <span class="tuile-lien">Consulter <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg></span>
          </a>
          <div class="tuile tuile-bientot">
            <span class="badge ton-neutre">Bientôt</span>
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg></span>
            <div>
              <h3>Mes sinistres</h3>
              <p>Déclarer un sinistre, envoyer vos justificatifs et suivre son traitement.</p>
            </div>
          </div>
          <div class="tuile tuile-bientot">
            <span class="badge ton-neutre">Bientôt</span>
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-carte"/></svg></span>
            <div>
              <h3>Mes paiements</h3>
              <p>Échéances à venir, paiements effectués et historique.</p>
            </div>
          </div>
        </div>
      </div>

      <section class="carte espace-haut">
        <div class="carte-entete">
          <div style="display:flex;align-items:center;gap:12px">
            <span class="pastille pastille-petite ton-violet"><svg class="icone" aria-hidden="true"><use href="#i-etincelles"/></svg></span>
            <div>
              <h2>Assistant</h2>
              <p>Posez vos questions sur vos contrats, vos garanties ou vos sinistres.</p>
            </div>
          </div>
          <span class="badge ton-neutre">Bientôt</span>
        </div>
        <div class="carte-corps">
          <p class="aide" style="margin-bottom:12px">Par exemple :</p>
          <div class="suggestions">
            <span class="suggestion"><svg class="icone" aria-hidden="true"><use href="#i-message"/></svg>Quels sont mes contrats actifs ?</span>
            <span class="suggestion"><svg class="icone" aria-hidden="true"><use href="#i-message"/></svg>Quelles garanties possède mon assurance automobile ?</span>
            <span class="suggestion"><svg class="icone" aria-hidden="true"><use href="#i-message"/></svg>Comment déclarer un sinistre ?</span>
            <span class="suggestion"><svg class="icone" aria-hidden="true"><use href="#i-message"/></svg>Où en est mon dernier sinistre ?</span>
          </div>
        </div>
      </section>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
