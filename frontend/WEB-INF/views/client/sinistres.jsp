<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Mes sinistres · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div class="entete-page">
        <div>
          <h1>Mes sinistres</h1>
          <p class="sous-titre">
            <c:choose>
              <c:when test="${empty sinistres}">Aucun sinistre déclaré.</c:when>
              <c:otherwise>${fn:length(sinistres)} sinistre${fn:length(sinistres) > 1 ? 's' : ''}, du plus récent au plus ancien.</c:otherwise>
            </c:choose>
          </p>
        </div>
        <div class="actions">
          <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/client/sinistres/nouveau">
            <svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg>Déclarer un sinistre
          </a>
        </div>
      </div>

      <%-- Erreur enregistrée avant la redirection (MessageFlash), par exemple un sinistre introuvable --%>
      <c:if test="${not empty sessionScope.messageErreur}">
        <div class="alerte ton-danger" role="alert">
          <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
          <div class="alerte-texte"><c:out value="${sessionScope.messageErreur}"/></div>
        </div>
        <c:remove var="messageErreur" scope="session"/>
      </c:if>

      <c:choose>
        <c:when test="${empty sinistres}">
          <div class="carte vide">
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg></span>
            <h2>Aucun sinistre déclaré</h2>
            <p>Accident, vol, dégât des eaux… Déclarez-le en ligne, joignez vos justificatifs et suivez son traitement ici.</p>
            <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/client/sinistres/nouveau">
              <svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg>Déclarer un sinistre
            </a>
          </div>
        </c:when>
        <c:otherwise>
          <c:if test="${fn:length(sinistres) > 1}">
            <div class="barre-outils avec-js" data-filtres="#liste-sinistres">
              <div class="puces" role="group" aria-label="Filtrer par statut">
                <button type="button" class="puce" data-filtre-valeur="" aria-pressed="true">Tous</button>
                <button type="button" class="puce" data-filtre-valeur="DECLARE" aria-pressed="false">Déclarés</button>
                <button type="button" class="puce" data-filtre-valeur="EN_ANALYSE" aria-pressed="false">En analyse</button>
                <button type="button" class="puce" data-filtre-valeur="ACCEPTE" aria-pressed="false">Acceptés</button>
                <button type="button" class="puce" data-filtre-valeur="INDEMNISE" aria-pressed="false">Indemnisés</button>
                <button type="button" class="puce" data-filtre-valeur="REFUSE" aria-pressed="false">Refusés</button>
              </div>
              <span class="compteur" data-filtre-compteur></span>
            </div>
          </c:if>

          <div class="pile" id="liste-sinistres">
            <c:forEach var="s" items="${sinistres}">
              <article class="carte contrat" data-filtre-element data-filtre-cle="${s.statut}">
                <div class="contrat-entete">
                  <div class="contrat-titre">
                    <span class="pastille ton-orange"><svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg></span>
                    <div>
                      <h2><app:typeSinistre valeur="${s.typeSinistre}"/></h2>
                      <p>Sinistre n° <c:out value="${s.id}"/> · contrat n° <c:out value="${s.contrat.id}"/>, <c:out value="${s.contrat.typeAssurance.nom}"/></p>
                    </div>
                  </div>
                  <app:statutSinistre valeur="${s.statut}"/>
                </div>
                <div class="contrat-infos">
                  <span>
                    <svg class="icone" aria-hidden="true"><use href="#i-calendrier"/></svg>
                    Survenu le <strong><app:date valeur="${s.dateSinistre}"/></strong>
                  </span>
                  <span>
                    <svg class="icone" aria-hidden="true"><use href="#i-euro"/></svg>
                    Estimé à <strong class="nombre"><app:euros valeur="${s.montantEstime}" vide="non précisé"/></strong>
                  </span>
                  <span>
                    <svg class="icone" aria-hidden="true"><use href="#i-utilisateur"/></svg>
                    <c:choose>
                      <c:when test="${empty s.agent}">En attente d'un agent</c:when>
                      <c:otherwise>Suivi par <c:out value="${s.agent.prenom} ${s.agent.nom}"/></c:otherwise>
                    </c:choose>
                  </span>
                </div>
                <div class="carte-pied">
                  <a class="bouton bouton-principal bouton-petit" href="${pageContext.request.contextPath}/client/sinistres/detail?id=${s.id}">
                    Suivre ce sinistre<svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg>
                  </a>
                </div>
              </article>
            </c:forEach>

            <div class="carte vide" data-filtre-vide hidden>
              <p>Aucun sinistre avec ce statut.</p>
            </div>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
