<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Mes paiements · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <c:set var="filtre" value="${not empty statutSelectionne or not empty modeSelectionne}"/>
  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div class="entete-page">
        <div>
          <h1>Mes paiements</h1>
          <p class="sous-titre">
            <c:choose>
              <c:when test="${empty paiements and not filtre}">Aucun paiement pour l'instant.</c:when>
              <c:otherwise>${fn:length(paiements)} paiement${fn:length(paiements) > 1 ? 's' : ''}${filtre ? ' avec ces critères' : ''}. Chaque contrat se paie en une fois, depuis sa fiche.</c:otherwise>
            </c:choose>
          </p>
        </div>
      </div>

      <%-- Filtres et tri faits par le serveur, en GET ; inutiles pour un client sans paiement --%>
      <c:if test="${not empty paiements or filtre}">
        <form class="barre-outils" method="get" action="${pageContext.request.contextPath}/client/paiements">
          <%@ include file="/WEB-INF/views/common/paiements-filtres.jspf" %>
          <button type="submit" class="bouton bouton-principal">Filtrer</button>
          <c:if test="${filtre}">
            <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/client/paiements">Effacer</a>
          </c:if>
        </form>
      </c:if>

      <c:choose>
        <c:when test="${empty paiements}">
          <div class="carte vide">
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-carte"/></svg></span>
            <h2>Aucun paiement</h2>
            <p>${filtre ? 'Aucun paiement ne correspond à ces critères.' : 'Le paiement de chacun de vos contrats apparaîtra ici.'}</p>
          </div>
        </c:when>
        <c:otherwise>
          <div class="tableau-conteneur">
            <table class="tableau">
              <thead>
                <tr>
                  <th>Contrat</th>
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
                  <%-- Le formulaire de paiement est sur la fiche du contrat, mêmes conditions que là-bas --%>
                  <c:set var="aPayer" value="${(p.statut == 'EN_ATTENTE' or p.statut == 'EN_RETARD')
                                               and p.contrat.statut != 'RESILIE' and p.contrat.statut != 'EXPIRE'}"/>
                  <tr>
                    <td>
                      <a class="cellule-principale" href="${pageContext.request.contextPath}/client/contrats/detail?id=${p.contrat.id}"><c:out value="${p.contrat.typeAssurance.nom}"/></a>
                      <div class="cellule-secondaire">Contrat n° <c:out value="${p.contrat.id}"/></div>
                    </td>
                    <td class="nombre"><app:date valeur="${p.dateEcheance}"/></td>
                    <td class="a-droite nombre cellule-principale"><app:euros valeur="${p.montant}"/></td>
                    <td class="nombre"><app:date valeur="${p.datePaiement}"/></td>
                    <td><c:choose><c:when test="${empty p.modePaiement}">—</c:when><c:otherwise><app:modePaiement valeur="${p.modePaiement}"/></c:otherwise></c:choose></td>
                    <td><app:statutPaiement valeur="${p.statut}"/></td>
                    <td class="cellule-actions">
                      <c:choose>
                        <c:when test="${aPayer}">
                          <a class="bouton bouton-petit bouton-principal" href="${pageContext.request.contextPath}/client/contrats/detail?id=${p.contrat.id}">
                            <svg class="icone" aria-hidden="true"><use href="#i-carte"/></svg>Payer
                          </a>
                        </c:when>
                        <c:otherwise>
                          <a class="bouton bouton-petit bouton-fantome" href="${pageContext.request.contextPath}/client/contrats/detail?id=${p.contrat.id}">
                            Voir le contrat<svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg>
                          </a>
                        </c:otherwise>
                      </c:choose>
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
