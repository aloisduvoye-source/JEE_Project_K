<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Changer mon mot de passe · Billy Assurance</title>
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
            <h1>Changer mon mot de passe</h1>
            <p class="sous-titre">Votre mot de passe actuel est demandé pour confirmer qu'il s'agit bien de vous.</p>
          </div>
        </div>
      </div>

      <form class="carte" method="post" action="${pageContext.request.contextPath}/compte/mot-de-passe" novalidate>
        <div class="carte-corps formulaire">
          <div class="champ ${not empty erreurs.actuel ? 'a-erreur' : ''}">
            <label for="actuel">Mot de passe actuel</label>
            <div class="saisie-groupe">
              <input class="saisie" type="password" id="actuel" name="actuel" autocomplete="current-password">
              <button type="button" class="saisie-bouton" data-voir-mdp aria-label="Afficher le mot de passe">
                <svg class="icone" aria-hidden="true"><use href="#i-oeil"/></svg>
              </button>
            </div>
            <c:if test="${not empty erreurs.actuel}"><span class="message-erreur"><c:out value="${erreurs.actuel}"/></span></c:if>
          </div>
          <div class="champs-2">
            <div class="champ ${not empty erreurs.nouveau ? 'a-erreur' : ''}">
              <label for="nouveau">Nouveau mot de passe</label>
              <div class="saisie-groupe">
                <input class="saisie" type="password" id="nouveau" name="nouveau" autocomplete="new-password" data-force>
                <button type="button" class="saisie-bouton" data-voir-mdp aria-label="Afficher le mot de passe">
                  <svg class="icone" aria-hidden="true"><use href="#i-oeil"/></svg>
                </button>
              </div>
              <c:if test="${not empty erreurs.nouveau}"><span class="message-erreur"><c:out value="${erreurs.nouveau}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.confirmation ? 'a-erreur' : ''}">
              <label for="confirmation">Confirmer le nouveau mot de passe</label>
              <input class="saisie" type="password" id="confirmation" name="confirmation" autocomplete="new-password" data-identique="#nouveau">
              <c:if test="${not empty erreurs.confirmation}"><span class="message-erreur"><c:out value="${erreurs.confirmation}"/></span></c:if>
            </div>
          </div>
        </div>
        <div class="carte-pied">
          <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/compte">Annuler</a>
          <button type="submit" class="bouton bouton-principal">Changer le mot de passe</button>
        </div>
      </form>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
