<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Mes contrats · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div class="entete-page">
        <div>
          <h1>Mes contrats</h1>
          <p class="sous-titre">
            <c:choose>
              <c:when test="${empty contrats and not recherche}">Aucun contrat pour l'instant.</c:when>
              <c:otherwise>${fn:length(contrats)} contrat${fn:length(contrats) > 1 ? 's' : ''}${recherche ? ' avec ces critères' : ''}.</c:otherwise>
            </c:choose>
          </p>
        </div>
      </div>

      <%-- Erreur enregistrée avant la redirection (MessageFlash), par exemple un contrat introuvable --%>
      <c:if test="${not empty sessionScope.messageErreur}">
        <div class="alerte ton-danger" role="alert">
          <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
          <div class="alerte-texte"><c:out value="${sessionScope.messageErreur}"/></div>
        </div>
        <c:remove var="messageErreur" scope="session"/>
      </c:if>

      <%-- Filtres et tri faits par le serveur, en GET ; inutiles pour un client sans contrat --%>
      <c:if test="${not empty contrats or recherche}">
        <form class="barre-outils" method="get" action="${pageContext.request.contextPath}/client/contrats">
          <%@ include file="/WEB-INF/views/common/contrats-filtres.jspf" %>
          <button type="submit" class="bouton bouton-principal">Filtrer</button>
          <c:if test="${recherche}">
            <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/client/contrats">Effacer</a>
          </c:if>
        </form>
      </c:if>

      <c:if test="${empty contrats}">
        <div class="carte vide">
          <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg></span>
          <c:choose>
            <c:when test="${recherche}">
              <h2>Aucun contrat trouvé</h2>
              <p>Aucun contrat ne correspond à ces critères.</p>
            </c:when>
            <c:otherwise>
              <h2>Vous n'avez aucun contrat</h2>
              <p>Vos contrats apparaîtront ici dès qu'un conseiller les aura créés pour vous.</p>
            </c:otherwise>
          </c:choose>
        </div>
      </c:if>

      <div class="pile">
        <c:forEach var="ct" items="${contrats}">
          <article class="carte contrat">
            <div class="contrat-entete">
              <div class="contrat-titre">
                <span class="pastille ton-primaire"><svg class="icone" aria-hidden="true"><use href="#i-bouclier"/></svg></span>
                <div>
                  <h2><c:out value="${ct.typeAssurance.nom}"/></h2>
                  <p>Contrat n° <c:out value="${ct.id}"/></p>
                </div>
              </div>
              <app:statutContrat valeur="${ct.statut}"/>
            </div>

            <div class="contrat-infos">
              <span>
                <svg class="icone" aria-hidden="true"><use href="#i-calendrier"/></svg>
                Du <strong><app:date valeur="${ct.dateDebut}"/></strong> au <strong><app:date valeur="${ct.dateFin}"/></strong>
              </span>
              <span>
                <svg class="icone" aria-hidden="true"><use href="#i-euro"/></svg>
                Prime annuelle <strong class="nombre"><app:euros valeur="${ct.prime}"/></strong>
              </span>
            </div>

            <%-- Part de la période déjà écoulée : calculée par app.js à partir des deux dates --%>
            <c:if test="${ct.statut == 'ACTIF'}">
              <div class="progression-zone avec-js" data-periode data-debut="${ct.dateDebut}" data-fin="${ct.dateFin}">
                <div class="progression-libelle"><span data-periode-texte></span><span data-periode-part></span></div>
                <div class="progression"><span></span></div>
              </div>
            </c:if>

            <div class="garanties">
              <c:forEach var="g" items="${ct.garanties}">
                <div class="garantie">
                  <span class="garantie-nom"><svg class="icone" aria-hidden="true"><use href="#i-bouclier-ok"/></svg><c:out value="${g.nom}"/></span>
                  <c:if test="${not empty g.description}"><p><c:out value="${g.description}"/></p></c:if>
                  <div class="garantie-chiffres">
                    <span>Plafond<b class="nombre"><app:euros valeur="${g.plafond}" vide="Aucun"/></b></span>
                    <span>Franchise<b class="nombre"><app:euros valeur="${g.franchise}" vide="Aucune"/></b></span>
                  </div>
                </div>
              </c:forEach>
            </div>

            <div class="carte-pied">
              <a class="bouton bouton-principal bouton-petit" href="${pageContext.request.contextPath}/client/contrats/detail?id=${ct.id}">
                Voir le contrat et son paiement<svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg>
              </a>
            </div>
          </article>
        </c:forEach>
      </div>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
