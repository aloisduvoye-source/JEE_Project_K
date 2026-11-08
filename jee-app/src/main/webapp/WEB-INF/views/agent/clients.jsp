<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Clients</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Clients</h1>

  <form method="get" action="${pageContext.request.contextPath}/agent/clients">
    <label for="q">Rechercher (nom, prénom ou email)</label>
    <input type="search" id="q" name="q" value="<c:out value='${q}'/>">
    <button type="submit">Rechercher</button>
    <c:if test="${not empty q}">
      <a href="${pageContext.request.contextPath}/agent/clients">Afficher tous les clients</a>
    </c:if>
    <c:out value="${erreurs.q}"/>
  </form>

  <table border="1">
    <thead>
      <tr>
        <th>Nom</th>
        <th>Prénom</th>
        <th>Email</th>
        <th>Téléphone</th>
        <th>Adresse</th>
        <th>Compte</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="client" items="${clients}">
        <tr>
          <td><c:out value="${client.utilisateur.nom}"/></td>
          <td><c:out value="${client.utilisateur.prenom}"/></td>
          <td><c:out value="${client.utilisateur.email}"/></td>
          <td><c:out value="${client.telephone}"/></td>
          <td><c:out value="${client.adresse}"/></td>
          <td>${client.utilisateur.actif ? 'Actif' : 'Désactivé'}</td>
        </tr>
      </c:forEach>
      <c:if test="${empty clients}">
        <tr>
          <td colspan="6">
            <c:choose>
              <c:when test="${not empty q}">Aucun client ne correspond à « <c:out value="${q}"/> ».</c:when>
              <c:otherwise>Aucun client pour l'instant.</c:otherwise>
            </c:choose>
          </td>
        </tr>
      </c:if>
    </tbody>
  </table>

  <%-- Page partagée agent / admin : retour vers l'espace du rôle connecté --%>
  <p><a href="${pageContext.request.contextPath}${sessionScope.utilisateur.role.accueil}">Retour à mon espace</a></p>
</body>
</html>
