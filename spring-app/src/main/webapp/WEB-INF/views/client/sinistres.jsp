<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Mes sinistres</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Mes sinistres</h1>

  <%-- Erreur enregistrée avant la redirection (MessageFlash), par exemple un sinistre introuvable --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <p><a href="${pageContext.request.contextPath}/client/sinistres/nouveau">Déclarer un sinistre</a></p>

  <table border="1">
    <thead>
      <tr>
        <th>Numéro</th>
        <th>Contrat</th>
        <th>Date du sinistre</th>
        <th>Type</th>
        <th>Montant estimé</th>
        <th>Statut</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="s" items="${sinistres}">
        <tr>
          <td><a href="${pageContext.request.contextPath}/client/sinistres/detail?id=${s.id}"><c:out value="${s.id}"/></a></td>
          <td>n° <c:out value="${s.contrat.id}"/> (<c:out value="${s.contrat.typeAssurance.nom}"/>)</td>
          <td><c:out value="${s.dateSinistre}"/></td>
          <td><c:out value="${s.typeSinistre}"/></td>
          <td><c:if test="${not empty s.montantEstime}"><c:out value="${s.montantEstime}"/> €</c:if></td>
          <td><c:out value="${s.statut}"/></td>
        </tr>
      </c:forEach>
      <c:if test="${empty sinistres}">
        <tr>
          <td colspan="6">Vous n'avez déclaré aucun sinistre.</td>
        </tr>
      </c:if>
    </tbody>
  </table>

  <p><a href="${pageContext.request.contextPath}/client/accueil">Retour à mon espace</a></p>
</body>
</html>
