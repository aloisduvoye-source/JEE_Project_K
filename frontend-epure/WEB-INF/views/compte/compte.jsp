<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Mon compte</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Mon compte</h1>

  <c:if test="${param.succes == 'profil'}">
    <p>Vos informations ont été mises à jour.</p>
  </c:if>
  <c:if test="${param.succes == 'motdepasse'}">
    <p>Votre mot de passe a été modifié.</p>
  </c:if>

  <p>Nom : <c:out value="${compte.nom}"/></p>
  <p>Prénom : <c:out value="${compte.prenom}"/></p>
  <p>Email : <c:out value="${compte.email}"/></p>
  <p>Identifiant : <c:out value="${compte.identifiant}"/></p>
  <p>Rôle : <c:out value="${compte.role}"/></p>

  <form method="get" action="${pageContext.request.contextPath}/compte/profil">
    <button type="submit">Modifier mes informations</button>
  </form>
  <form method="get" action="${pageContext.request.contextPath}/compte/mot-de-passe">
    <button type="submit">Changer mon mot de passe</button>
  </form>
  <%-- L'administrateur ne peut pas supprimer son compte : il est le seul à gérer l'application --%>
  <c:if test="${compte.role != 'ADMIN'}">
    <form method="get" action="${pageContext.request.contextPath}/compte/supprimer">
      <button type="submit">Supprimer mon compte</button>
    </form>
  </c:if>

  <p><a href="${pageContext.request.contextPath}/">Retour à l'accueil</a></p>
</body>
</html>
