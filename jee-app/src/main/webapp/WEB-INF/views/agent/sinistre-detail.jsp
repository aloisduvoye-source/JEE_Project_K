<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Sinistre n° <c:out value="${sinistre.id}"/></title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Sinistre n° <c:out value="${sinistre.id}"/></h1>

  <%-- Erreur enregistrée avant la redirection (MessageFlash), affichée une seule fois --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <%@ include file="/WEB-INF/views/common/sinistre-infos.jspf" %>
  <p>Client : <c:out value="${sinistre.contrat.client.utilisateur.email}"/>
    (<a href="${pageContext.request.contextPath}/agent/contrats/detail?id=${sinistre.contrat.id}">voir le contrat</a>)</p>

  <h2>Garanties du contrat</h2>
  <%-- Indemnisation suggérée : montant estimé moins la franchise, dans la limite du plafond --%>
  <table border="1">
    <thead>
      <tr>
        <th>Garantie</th>
        <th>Plafond</th>
        <th>Franchise</th>
        <th>Indemnisation suggérée</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="p" items="${propositions}">
        <tr>
          <td><c:out value="${p.garantie.nom}"/></td>
          <td><c:out value="${empty p.garantie.plafond ? '-' : p.garantie.plafond}"/></td>
          <td><c:out value="${empty p.garantie.franchise ? '-' : p.garantie.franchise}"/></td>
          <td><c:out value="${empty p.montant ? 'pas de montant estimé' : p.montant.toPlainString().concat(' €')}"/></td>
        </tr>
      </c:forEach>
      <c:if test="${empty propositions}">
        <tr>
          <td colspan="4">Ce contrat n'a aucune garantie : le sinistre ne peut normalement pas être couvert.</td>
        </tr>
      </c:if>
    </tbody>
  </table>

  <%@ include file="/WEB-INF/views/common/sinistre-discussion.jspf" %>

  <%-- Actions selon le statut, pour un agent seulement : l'admin consulte.
       ActionSinistreServlet et SinistreService vérifient de toute façon le rôle et le statut. --%>
  <c:if test="${sessionScope.utilisateur.role == 'AGENT'}">
    <c:set var="actionUrl" value="${pageContext.request.contextPath}/agent/sinistres/action"/>
    <c:choose>
      <c:when test="${sinistre.statut == 'DECLARE'}">
        <h2>Traitement</h2>
        <form method="post" action="${actionUrl}">
          <input type="hidden" name="id" value="${sinistre.id}">
          <input type="hidden" name="action" value="prendre">
          <button type="submit">Prendre en charge ce dossier</button>
        </form>
      </c:when>

      <c:when test="${sinistre.statut == 'EN_ANALYSE'}">
        <h2>Décision</h2>
        <form method="post" action="${actionUrl}"
              onsubmit="return confirm('Accepter ce sinistre ? Il pourra ensuite être indemnisé.');">
          <input type="hidden" name="id" value="${sinistre.id}">
          <input type="hidden" name="action" value="accepter">
          <button type="submit">Accepter le sinistre</button>
        </form>
        <form method="post" action="${actionUrl}"
              onsubmit="return confirm('Refuser ce sinistre ? Cette décision est définitive.');">
          <input type="hidden" name="id" value="${sinistre.id}">
          <input type="hidden" name="action" value="refuser">
          <p>
            <label for="motif">Motif du refus (visible par le client)</label><br>
            <textarea id="motif" name="motif" rows="3" cols="70" maxlength="1000" required></textarea>
          </p>
          <button type="submit">Refuser le sinistre</button>
        </form>
      </c:when>

      <c:when test="${sinistre.statut == 'ACCEPTE'}">
        <h2>Indemnisation</h2>
        <p>Le versement est enregistré à la date du jour. Le dossier est ensuite clos.</p>
        <form method="post" action="${actionUrl}"
              onsubmit="return confirm('Enregistrer cette indemnisation ? Cette action est définitive.');">
          <input type="hidden" name="id" value="${sinistre.id}">
          <input type="hidden" name="action" value="indemniser">
          <p>
            <label for="montant">Montant versé, en €</label>
            <input type="text" id="montant" name="montant" inputmode="decimal" required>
          </p>
          <button type="submit">Indemniser le client</button>
        </form>
      </c:when>
    </c:choose>
  </c:if>

  <p><a href="${pageContext.request.contextPath}/agent/sinistres">Retour aux sinistres</a></p>
</body>
</html>
