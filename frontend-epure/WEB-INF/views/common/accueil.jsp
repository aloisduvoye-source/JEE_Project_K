<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <%-- Après « Supprimer mon compte » (SuppressionCompteServlet) --%>
  <c:if test="${param.compte == 'supprime'}">
    <p>Votre compte a été supprimé.</p>
  </c:if>
  <c:if test="${param.compte == 'desactive'}">
    <p>Votre compte a été désactivé : vos contrats et sinistres sont conservés par l'assurance.</p>
  </c:if>

  <h1>Billy Assurance</h1>
  <p>Bienvenue sur le site de notre assurance en ligne.</p>

</body>
</html>
