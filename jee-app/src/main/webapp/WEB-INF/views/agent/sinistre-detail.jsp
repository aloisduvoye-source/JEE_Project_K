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

  <p><a href="${pageContext.request.contextPath}/agent/sinistres">Retour aux sinistres</a></p>
</body>
</html>
