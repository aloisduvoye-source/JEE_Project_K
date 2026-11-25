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

  <c:if test="${not empty param.agentCree}">
    <p>Le compte agent a été créé.</p>
  </c:if>
  <c:if test="${not empty param.desactive}">
    <p>Le compte a été désactivé : il ne peut plus se connecter, son historique est conservé.</p>
  </c:if>
  <c:if test="${not empty param.reactive}">
    <p>Le compte a été réactivé.</p>
  </c:if>
  <%-- Erreur enregistrée avant la redirection (MessageFlash), affichée une seule fois --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <p><a href="${pageContext.request.contextPath}/admin/utilisateurs/nouvel-agent">Nouvel agent</a></p>

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
        <th>Actions</th>
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
          <td>
            <%-- Pas de bouton pour l'administrateur : le service refuse de toute façon de le désactiver --%>
            <c:if test="${u.role != 'ADMIN'}">
              <form method="post" action="${pageContext.request.contextPath}/admin/utilisateurs/${u.actif ? 'desactiver' : 'reactiver'}"
                    <c:if test="${u.actif}">onsubmit="return confirm('Désactiver ce compte ? Il ne pourra plus se connecter.');"</c:if>>
                <input type="hidden" name="id" value="${u.id}">
                <input type="hidden" name="role" value="${role}">
                <button type="submit">${u.actif ? 'Désactiver' : 'Réactiver'}</button>
              </form>
            </c:if>
          </td>
        </tr>
      </c:forEach>
      <c:if test="${empty utilisateurs}">
        <tr>
          <td colspan="8">Aucun utilisateur à afficher.</td>
        </tr>
      </c:if>
    </tbody>
  </table>

  <p><a href="${pageContext.request.contextPath}/admin/accueil">Retour à l'espace administrateur</a></p>
</body>
</html>
