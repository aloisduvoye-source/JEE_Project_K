<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Créer un compte</title>
</head>
<body>

  <h1>Créer un compte</h1>

  <form method="post" action="${pageContext.request.contextPath}/inscription">
    <p>
      <label for="identifiant">Identifiant *</label>
      <input type="text" id="identifiant" name="identifiant" value="<c:out value='${identifiant}'/>" autocomplete="username">
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
      <label for="telephone">Téléphone</label>
      <input type="tel" id="telephone" name="telephone" value="<c:out value='${telephone}'/>">
      <c:out value="${erreurs.telephone}"/>
    </p>
    <p>
      <label for="adresse">Adresse</label>
      <input type="text" id="adresse" name="adresse" value="<c:out value='${adresse}'/>">
      <c:out value="${erreurs.adresse}"/>
    </p>
    <p>
      <label for="dateNaissance">Date de naissance</label>
      <input type="date" id="dateNaissance" name="dateNaissance" value="<c:out value='${dateNaissance}'/>">
      <c:out value="${erreurs.dateNaissance}"/>
    </p>
    <p>
      <label for="motDePasse">Mot de passe * (8 caractères minimum)</label>
      <input type="password" id="motDePasse" name="motDePasse" autocomplete="new-password">
      <c:out value="${erreurs.motDePasse}"/>
    </p>
    <p>
      <label for="confirmation">Confirmer le mot de passe *</label>
      <input type="password" id="confirmation" name="confirmation" autocomplete="new-password">
      <c:out value="${erreurs.confirmation}"/>
    </p>
    <p>* champs obligatoires</p>
    <button type="submit">Créer mon compte</button>
  </form>

  <p>Déjà un compte ? <a href="${pageContext.request.contextPath}/connexion">Se connecter</a></p>
  <p>Retour a l'<a href="${pageContext.request.contextPath}/">accueil</a></p>

</body>
</html>
