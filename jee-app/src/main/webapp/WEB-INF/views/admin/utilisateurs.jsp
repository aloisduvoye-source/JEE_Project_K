<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Utilisateurs</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Utilisateurs</h1>

  <form method="get" action="${pageContext.request.contextPath}/admin/utilisateurs">
    <label for="role">Rôle</label>
    <select id="role" name="role">
      <option value="">Tous</option>
      <c:forEach var="r" items="${roles}">
        <option value="${r}" ${r == role ? 'selected' : ''}>${r}</option>
      </c:forEach>
    </select>
    <button type="submit">Filtrer</button>
  </form>

  <table border="1">
    <thead>
      <tr>
        <th>Nom</th>
        <th>Prénom</th>
        <th>Email</th>
        <th>Identifiant</th>
        <th>Rôle</th>
        <th>Compte</th>
        <th>Inscription</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="u" items="${utilisateurs}">
        <tr>
          <td><c:out value="${u.nom}"/></td>
          <td><c:out value="${u.prenom}"/></td>
          <td><c:out value="${u.email}"/></td>
          <td><c:out value="${u.identifiant}"/></td>
          <td>${u.role}</td>
          <td>${u.actif ? 'Actif' : 'Désactivé'}</td>
          <%-- Date seule (AAAA-MM-JJ), sans l'heure --%>
          <td>${u.dateCreation.toLocalDate()}</td>
        </tr>
      </c:forEach>
      <c:if test="${empty utilisateurs}">
        <tr>
          <td colspan="7">Aucun utilisateur à afficher.</td>
        </tr>
      </c:if>
    </tbody>
  </table>

  <p><a href="${pageContext.request.contextPath}/admin/accueil">Retour à l'espace administrateur</a></p>
</body>
</html>
