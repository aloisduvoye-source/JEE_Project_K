<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Sinistres · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div class="entete-page">
        <div>
          <h1>Sinistres</h1>
          <p class="sous-titre">
            ${fn:length(sinistres)} sinistre${fn:length(sinistres) > 1 ? 's' : ''}
            <c:choose>
              <c:when test="${statutChoisi == 'DECLARE'}">à prendre en charge, du plus ancien au plus récent.</c:when>
              <c:when test="${not empty statutChoisi}">avec ce statut, du plus ancien au plus récent.</c:when>
              <c:otherwise>${mien ? 'dans vos dossiers' : 'au total'}.</c:otherwise>
            </c:choose>
          </p>
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

      <%-- Filtres dans l'adresse (?statut=..., ?mien=1) : la liste est filtrée par le serveur,
           et la page peut être gardée en favori (ex. la file des sinistres à prendre en charge) --%>
      <div class="barre-outils">
        <div class="puces" role="group" aria-label="Filtrer par statut">
          <c:url var="urlTous" value="/agent/sinistres"><c:if test="${mien}"><c:param name="mien" value="1"/></c:if></c:url>
          <a class="puce" href="${urlTous}" ${empty statutChoisi ? 'aria-current="page"' : ''}>Tous</a>
          <c:forEach var="st" items="${statuts}">
            <c:url var="urlStatut" value="/agent/sinistres"><c:param name="statut" value="${st}"/><c:if test="${mien}"><c:param name="mien" value="1"/></c:if></c:url>
            <a class="puce" href="${urlStatut}" ${st == statutChoisi ? 'aria-current="page"' : ''}>
              <c:choose>
                <c:when test="${st == 'DECLARE'}">À prendre en charge</c:when>
                <c:when test="${st == 'EN_ANALYSE'}">En analyse</c:when>
                <c:when test="${st == 'ACCEPTE'}">À indemniser</c:when>
                <c:when test="${st == 'REFUSE'}">Refusés</c:when>
                <c:when test="${st == 'INDEMNISE'}">Indemnisés</c:when>
              </c:choose>
            </a>
          </c:forEach>
        </div>
        <%-- Seul un agent a des dossiers attribués : l'admin consulte sans ce filtre --%>
        <c:if test="${sessionScope.utilisateur.role == 'AGENT'}">
          <c:url var="urlMien" value="/agent/sinistres"><c:if test="${not empty statutChoisi}"><c:param name="statut" value="${statutChoisi}"/></c:if><c:if test="${not mien}"><c:param name="mien" value="1"/></c:if></c:url>
          <a class="puce" href="${urlMien}" ${mien ? 'aria-current="page"' : ''}>
            <svg class="icone" aria-hidden="true" style="width:14px;height:14px;margin-right:6px"><use href="#i-utilisateur"/></svg>Mes dossiers
          </a>
        </c:if>
      </div>

      <c:choose>
        <c:when test="${empty sinistres}">
          <div class="carte vide">
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg></span>
            <h2>Aucun sinistre</h2>
            <p>${statutChoisi == 'DECLARE' ? 'Tous les sinistres déclarés ont été pris en charge.' : 'Aucun sinistre ne correspond à ces filtres.'}</p>
          </div>
        </c:when>
        <c:otherwise>
          <div>
            <div class="barre-outils avec-js" data-filtres="#tableau-sinistres">
              <label class="recherche">
                <span class="visuellement-cache">Rechercher un sinistre</span>
                <svg class="icone" aria-hidden="true"><use href="#i-recherche"/></svg>
                <input class="saisie" type="search" placeholder="Assuré, type, numéro…" data-filtre-texte>
              </label>
              <span class="compteur" data-filtre-compteur></span>
            </div>

            <div class="tableau-conteneur">
              <table class="tableau" id="tableau-sinistres">
                <thead>
                  <tr>
                    <th>N°</th>
                    <th>Assuré</th>
                    <th>Type</th>
                    <th>Survenu le</th>
                    <th class="a-droite">Estimé</th>
                    <th>Agent</th>
                    <th>Statut</th>
                    <th><span class="visuellement-cache">Actions</span></th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="s" items="${sinistres}">
                    <c:set var="assure" value="${s.contrat.client.utilisateur}"/>
                    <tr data-filtre-element data-filtre-cle="${s.statut}">
                      <td class="nombre"><a class="cellule-principale" href="${pageContext.request.contextPath}/agent/sinistres/detail?id=${s.id}">#<c:out value="${s.id}"/></a></td>
                      <td>
                        <div class="cellule-personne">
                          <span class="avatar"><c:out value="${fn:toUpperCase(fn:substring(assure.prenom, 0, 1))}${fn:toUpperCase(fn:substring(assure.nom, 0, 1))}"/></span>
                          <div>
                            <div class="cellule-principale"><c:out value="${assure.prenom} ${assure.nom}"/></div>
                            <div class="cellule-secondaire">Contrat n° <c:out value="${s.contrat.id}"/></div>
                          </div>
                        </div>
                      </td>
                      <td style="white-space:nowrap"><app:typeSinistre valeur="${s.typeSinistre}"/></td>
                      <td class="nombre"><app:date valeur="${s.dateSinistre}"/></td>
                      <td class="a-droite nombre"><app:euros valeur="${s.montantEstime}" vide="—"/></td>
                      <td style="white-space:nowrap"><c:out value="${empty s.agent ? '—' : s.agent.prenom.concat(' ').concat(s.agent.nom)}"/></td>
                      <td><app:statutSinistre valeur="${s.statut}"/></td>
                      <td class="cellule-actions">
                        <a class="bouton bouton-petit bouton-fantome" href="${pageContext.request.contextPath}/agent/sinistres/detail?id=${s.id}">
                          ${sessionScope.utilisateur.role == 'AGENT' and s.statut != 'REFUSE' and s.statut != 'INDEMNISE' ? 'Traiter' : 'Voir'}<svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg>
                        </a>
                      </td>
                    </tr>
                  </c:forEach>
                  <tr class="ligne-message" data-filtre-vide hidden>
                    <td colspan="8">Aucun sinistre ne correspond à la recherche.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
