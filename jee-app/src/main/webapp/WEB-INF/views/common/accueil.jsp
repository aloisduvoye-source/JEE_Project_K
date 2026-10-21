<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Assurance</title>
</head>
<body>
  <h1>Hello World</h1>

  <c:if test="${not empty sessionScope.utilisateur}">
    <p>
      Bonjour <c:out value="${sessionScope.utilisateur.nom} ${sessionScope.utilisateur.prenom}"/>,
      vous êtes connecté en tant que <c:out value="${sessionScope.utilisateur.role}"/>
    </p>
    <form method="get" action="${pageContext.request.contextPath}${sessionScope.utilisateur.role.accueil}">
      <button type="submit">Mon espace</button>
    </form>
    <form method="get" action="${pageContext.request.contextPath}/compte">
      <button type="submit">Mon compte</button>
    </form>
    <form method="post" action="${pageContext.request.contextPath}/deconnexion">
      <button type="submit">Se déconnecter</button>
    </form>
  </c:if>

  <c:if test="${empty sessionScope.utilisateur}">
    <form method="get" action="${pageContext.request.contextPath}/connexion">
      <button type="submit">Se connecter</button>
    </form>
  </c:if>
</body>
</html>
