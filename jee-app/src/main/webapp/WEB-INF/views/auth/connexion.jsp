<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Assurance</title>
</head>
<body>

    <h1>Connexion</h1>
    <p>${erreur}</p>
    <form method="post" action="${pageContext.request.contextPath}/connexion">
        <input type="text" name="identifiant">
        <input type="password" name="motDePasse">
        <button type="submit">Se Connecter</button>
    </form>
    <p>Pas encore de compte ? <a href="${pageContext.request.contextPath}/inscription">Créer un compte</a></p>
    <p>Retour a l'<a href="${pageContext.request.contextPath}/">accueil</a></p>

</body>
</html>
