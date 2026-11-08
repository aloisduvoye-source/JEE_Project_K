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
              <c:when test="${empty contrats}">Aucun contrat pour l'instant.</c:when>
              <c:otherwise>${fn:length(contrats)} contrat${fn:length(contrats) > 1 ? 's' : ''}, du plus récent au plus ancien.</c:otherwise>
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

      <c:if test="${empty contrats}">
        <div class="carte vide">
          <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg></span>
          <h2>Vous n'avez aucun contrat</h2>
          <p>Vos contrats apparaîtront ici dès qu'un conseiller les aura créés pour vous.</p>
        </div>
      </c:if>

      <c:if test="${fn:length(contrats) > 1}">
        <div class="barre-outils avec-js" data-filtres="#liste-contrats">
          <div class="puces" role="group" aria-label="Filtrer par statut">
            <button type="button" class="puce" data-filtre-valeur="" aria-pressed="true">Tous</button>
            <button type="button" class="puce" data-filtre-valeur="ACTIF" aria-pressed="false">Actifs</button>
            <button type="button" class="puce" data-filtre-valeur="EN_ATTENTE" aria-pressed="false">En attente</button>
            <button type="button" class="puce" data-filtre-valeur="SUSPENDU" aria-pressed="false">Suspendus</button>
            <button type="button" class="puce" data-filtre-valeur="EXPIRE" aria-pressed="false">Expirés</button>
            <button type="button" class="puce" data-filtre-valeur="RESILIE" aria-pressed="false">Résiliés</button>
          </div>
          <span class="compteur" data-filtre-compteur></span>
        </div>
      </c:if>

      <div class="pile" id="liste-contrats">
        <c:forEach var="ct" items="${contrats}">
          <article class="carte contrat" data-filtre-element data-filtre-cle="${ct.statut}">
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

        <div class="carte vide" data-filtre-vide hidden>
          <p>Aucun contrat avec ce statut.</p>
        </div>
      </div>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
