<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Changer mon mot de passe</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Changer mon mot de passe</h1>

  <form method="post" action="${pageContext.request.contextPath}/compte/mot-de-passe">
    <p>
      <label for="actuel">Mot de passe actuel</label>
      <input type="password" id="actuel" name="actuel" autocomplete="current-password">
      <c:out value="${erreurs.actuel}"/>
    </p>
    <p>
      <label for="nouveau">Nouveau mot de passe</label>
      <input type="password" id="nouveau" name="nouveau" autocomplete="new-password">
      <c:out value="${erreurs.nouveau}"/>
    </p>
    <p>
      <label for="confirmation">Confirmer le nouveau mot de passe</label>
      <input type="password" id="confirmation" name="confirmation" autocomplete="new-password">
      <c:out value="${erreurs.confirmation}"/>
    </p>
    <button type="submit">Changer le mot de passe</button>
  </form>

  <p><a href="${pageContext.request.contextPath}/compte">Annuler</a></p>
</body>
</html>
