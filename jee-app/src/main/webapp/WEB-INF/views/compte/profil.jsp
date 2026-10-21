<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Modifier mes informations</title>
</head>
<body>
  <h1>Modifier mes informations</h1>

  <form method="post" action="${pageContext.request.contextPath}/compte/profil">
    <p>
      <label for="nom">Nom</label>
      <input type="text" id="nom" name="nom" value="<c:out value='${nom}'/>">
      <c:out value="${erreurs.nom}"/>
    </p>
    <p>
      <label for="prenom">Prénom</label>
      <input type="text" id="prenom" name="prenom" value="<c:out value='${prenom}'/>">
      <c:out value="${erreurs.prenom}"/>
    </p>
    <p>
      <label for="email">Email</label>
      <input type="email" id="email" name="email" value="<c:out value='${email}'/>">
      <c:out value="${erreurs.email}"/>
    </p>
    <button type="submit">Enregistrer</button>
  </form>

  <p><a href="${pageContext.request.contextPath}/compte">Annuler</a></p>
</body>
</html>
