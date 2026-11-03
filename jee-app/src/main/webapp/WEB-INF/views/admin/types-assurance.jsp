<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Types d'assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Types d'assurance</h1>

  <table border="1">
    <thead>
      <tr>
        <th>Nom</th>
        <th>Description</th>
        <th>Prix de base</th>
        <th>Garanties disponibles</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="type" items="${types}">
        <tr>
          <td><c:out value="${type.nom}"/></td>
          <td><c:out value="${type.description}"/></td>
          <td><c:out value="${type.prixBase}"/> €</td>
          <td>
            <%-- Noms des garanties séparés par des virgules --%>
            <c:forEach var="g" items="${type.garanties}" varStatus="etat">
              <c:out value="${g.nom}"/><c:if test="${not etat.last}">, </c:if>
            </c:forEach>
            <c:if test="${empty type.garanties}">Aucune</c:if>
          </td>
        </tr>
      </c:forEach>
      <c:if test="${empty types}">
        <tr>
          <td colspan="4">Aucun type d'assurance pour l'instant.</td>
        </tr>
      </c:if>
    </tbody>
  </table>

  <p><a href="${pageContext.request.contextPath}/admin/accueil">Retour à l'espace administrateur</a></p>
</body>
</html>
