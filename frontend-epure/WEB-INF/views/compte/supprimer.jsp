<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Supprimer mon compte</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Supprimer mon compte</h1>

  <p>Vous serez déconnecté et ne pourrez plus vous connecter avec ce compte.</p>
  <p>
    Si votre compte est lié à des contrats ou à des sinistres, l'assurance doit les conserver :
    il sera alors désactivé au lieu d'être effacé. Sinon, il sera effacé définitivement.
  </p>

  <form method="post" action="${pageContext.request.contextPath}/compte/supprimer">
    <p>
      <label for="motDePasse">Mot de passe, pour confirmer</label>
      <input type="password" id="motDePasse" name="motDePasse" autocomplete="current-password">
      <c:out value="${erreur}"/>
    </p>
    <button type="submit">Supprimer définitivement mon compte</button>
  </form>

  <p><a href="${pageContext.request.contextPath}/compte">Annuler</a></p>
</body>
</html>
