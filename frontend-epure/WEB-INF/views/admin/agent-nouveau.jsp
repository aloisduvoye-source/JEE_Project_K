<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Nouvel agent</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Nouvel agent</h1>

  <form method="post" action="${pageContext.request.contextPath}/admin/utilisateurs/nouvel-agent">
    <p>
      <label for="identifiant">Identifiant *</label>
      <input type="text" id="identifiant" name="identifiant" value="<c:out value='${identifiant}'/>" autocomplete="off">
      <c:out value="${erreurs.identifiant}"/>
    </p>
    <p>
      <label for="nom">Nom *</label>
      <input type="text" id="nom" name="nom" value="<c:out value='${nom}'/>">
      <c:out value="${erreurs.nom}"/>
    </p>
    <p>
      <label for="prenom">Prénom *</label>
      <input type="text" id="prenom" name="prenom" value="<c:out value='${prenom}'/>">
      <c:out value="${erreurs.prenom}"/>
    </p>
    <p>
      <label for="email">Email *</label>
      <input type="email" id="email" name="email" value="<c:out value='${email}'/>">
      <c:out value="${erreurs.email}"/>
    </p>
    <p>
      <label for="motDePasse">Mot de passe provisoire * (8 caractères minimum)</label>
      <input type="password" id="motDePasse" name="motDePasse" autocomplete="new-password">
      <c:out value="${erreurs.motDePasse}"/>
    </p>
    <p>
      <label for="confirmation">Confirmer le mot de passe *</label>
      <input type="password" id="confirmation" name="confirmation" autocomplete="new-password">
      <c:out value="${erreurs.confirmation}"/>
    </p>
    <p>* champs obligatoires. L'agent pourra changer son mot de passe depuis « Mon compte ».</p>
    <button type="submit">Créer le compte agent</button>
  </form>

  <p><a href="${pageContext.request.contextPath}/admin/utilisateurs">Retour à la liste des utilisateurs</a></p>
</body>
</html>
