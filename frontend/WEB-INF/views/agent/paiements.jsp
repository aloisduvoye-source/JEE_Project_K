<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Paiements · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <c:set var="filtre" value="${not empty statutSelectionne or not empty modeSelectionne}"/>
  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div class="entete-page">
        <div>
          <h1>Paiements</h1>
          <p class="sous-titre">${fn:length(paiements)} paiement${fn:length(paiements) > 1 ? 's' : ''}${filtre ? ' avec ces critères' : ' au total'}.</p>
        </div>
      </div>

      <%-- Filtres et tri faits par le serveur, en GET : l'adresse peut être gardée en favori (ex. les retards) --%>
      <form class="barre-outils" method="get" action="${pageContext.request.contextPath}/agent/paiements">
        <%@ include file="/WEB-INF/views/common/paiements-filtres.jspf" %>
        <button type="submit" class="bouton bouton-principal">Filtrer</button>
        <c:if test="${filtre}">
          <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/agent/paiements">Effacer</a>
        </c:if>
      </form>

      <c:choose>
        <c:when test="${empty paiements}">
          <div class="carte vide">
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-carte"/></svg></span>
            <h2>Aucun paiement</h2>
            <p>${filtre ? 'Aucun paiement ne correspond à ces critères.' : 'Les paiements des contrats apparaîtront ici.'}</p>
          </div>
        </c:when>
        <c:otherwise>
          <div class="tableau-conteneur">
            <table class="tableau">
              <thead>
                <tr>
                  <th>Contrat</th>
                  <th>Client</th>
                  <th>Échéance</th>
                  <th class="a-droite">Montant</th>
                  <th>Payé le</th>
                  <th>Mode</th>
                  <th>Statut</th>
                  <th><span class="visuellement-cache">Actions</span></th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="p" items="${paiements}">
                  <c:set var="assure" value="${p.contrat.client.utilisateur}"/>
                  <tr>
                    <td>
                      <a class="cellule-principale" href="${pageContext.request.contextPath}/agent/contrats/detail?id=${p.contrat.id}">Contrat n° <c:out value="${p.contrat.id}"/></a>
                      <div class="cellule-secondaire"><c:out value="${p.contrat.typeAssurance.nom}"/></div>
                    </td>
                    <td>
                      <div class="cellule-personne">
                        <span class="avatar"><c:out value="${fn:toUpperCase(fn:substring(assure.prenom, 0, 1))}${fn:toUpperCase(fn:substring(assure.nom, 0, 1))}"/></span>
                        <div class="cellule-principale"><c:out value="${assure.prenom} ${assure.nom}"/></div>
                      </div>
                    </td>
                    <td class="nombre"><app:date valeur="${p.dateEcheance}"/></td>
                    <td class="a-droite nombre cellule-principale"><app:euros valeur="${p.montant}"/></td>
                    <td class="nombre"><app:date valeur="${p.datePaiement}"/></td>
                    <td><c:choose><c:when test="${empty p.modePaiement}">—</c:when><c:otherwise><app:modePaiement valeur="${p.modePaiement}"/></c:otherwise></c:choose></td>
                    <td><app:statutPaiement valeur="${p.statut}"/></td>
                    <td class="cellule-actions">
                      <a class="bouton bouton-petit bouton-fantome" href="${pageContext.request.contextPath}/agent/contrats/detail?id=${p.contrat.id}">
                        Voir le contrat<svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg>
                      </a>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
