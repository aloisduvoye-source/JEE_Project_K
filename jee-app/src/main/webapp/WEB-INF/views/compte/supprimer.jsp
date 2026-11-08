<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Supprimer mon compte · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur conteneur-etroit apparition">
      <div>
        <a class="retour" href="${pageContext.request.contextPath}/compte">
          <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Mon compte
        </a>
        <div class="entete-page">
          <div>
            <h1>Supprimer mon compte</h1>
            <p class="sous-titre">Vous serez déconnecté et ne pourrez plus vous connecter avec ce compte.</p>
          </div>
        </div>
      </div>

      <div class="alerte ton-attention" role="note" data-permanente>
        <svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg>
        <div class="alerte-texte">
          <strong>Que deviennent vos données ?</strong>
          Si votre compte est lié à des contrats ou à des sinistres, l'assurance doit les conserver :
          il sera alors désactivé au lieu d'être effacé. Sinon, il sera effacé définitivement.
        </div>
      </div>

      <form class="carte" method="post" action="${pageContext.request.contextPath}/compte/supprimer" novalidate>
        <div class="carte-corps formulaire">
          <div class="champ ${not empty erreur ? 'a-erreur' : ''}">
            <label for="motDePasse">Mot de passe, pour confirmer</label>
            <div class="saisie-groupe">
              <input class="saisie" type="password" id="motDePasse" name="motDePasse" autocomplete="current-password" autofocus>
              <button type="button" class="saisie-bouton" data-voir-mdp aria-label="Afficher le mot de passe">
                <svg class="icone" aria-hidden="true"><use href="#i-oeil"/></svg>
              </button>
            </div>
            <c:if test="${not empty erreur}"><span class="message-erreur"><c:out value="${erreur}"/></span></c:if>
          </div>
        </div>
        <div class="carte-pied">
          <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/compte">Annuler</a>
          <button type="submit" class="bouton bouton-danger">
            <svg class="icone" aria-hidden="true"><use href="#i-poubelle"/></svg>Supprimer définitivement mon compte
          </button>
        </div>
      </form>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
