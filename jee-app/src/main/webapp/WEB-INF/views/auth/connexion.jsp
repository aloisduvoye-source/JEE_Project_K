<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Connexion · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="auth">
    <div class="auth-carte apparition">
      <div class="auth-entete">
        <span class="logo-icone"><svg class="icone" aria-hidden="true"><use href="#i-bouclier"/></svg></span>
        <h1>Connexion</h1>
        <p>Accédez à votre espace personnel.</p>
      </div>

      <div class="carte carte-corps">
        <c:if test="${not empty erreur}">
          <div class="alerte ton-danger" role="alert">
            <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
            <div class="alerte-texte"><c:out value="${erreur}"/></div>
          </div>
        </c:if>

        <form class="formulaire" method="post" action="${pageContext.request.contextPath}/connexion">
          <div class="champ">
            <label for="identifiant">Identifiant</label>
            <%-- Après une erreur, l'identifiant saisi est réaffiché (jamais le mot de passe) --%>
            <input class="saisie" type="text" id="identifiant" name="identifiant" autocomplete="username"
                   value="<c:out value='${param.identifiant}'/>" autofocus required>
          </div>
          <div class="champ">
            <label for="motDePasse">Mot de passe</label>
            <div class="saisie-groupe">
              <input class="saisie" type="password" id="motDePasse" name="motDePasse" autocomplete="current-password" required>
              <button type="button" class="saisie-bouton" data-voir-mdp aria-label="Afficher le mot de passe">
                <svg class="icone" aria-hidden="true"><use href="#i-oeil"/></svg>
              </button>
            </div>
          </div>
          <button type="submit" class="bouton bouton-principal bouton-grand bouton-bloc">Se connecter</button>
        </form>
      </div>

      <p class="auth-pied">
        Pas encore de compte ? <a href="${pageContext.request.contextPath}/inscription">Créer un compte</a>
      </p>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
