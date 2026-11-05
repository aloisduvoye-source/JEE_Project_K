<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Modifier mes informations · Billy Assurance</title>
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
            <h1>Modifier mes informations</h1>
            <p class="sous-titre">Votre identifiant de connexion ne change pas.</p>
          </div>
        </div>
      </div>

      <form class="carte" method="post" action="${pageContext.request.contextPath}/compte/profil" novalidate>
        <div class="carte-corps formulaire">
          <div class="champs-2">
            <div class="champ ${not empty erreurs.prenom ? 'a-erreur' : ''}">
              <label for="prenom">Prénom</label>
              <input class="saisie" type="text" id="prenom" name="prenom" autocomplete="given-name" value="<c:out value='${prenom}'/>">
              <c:if test="${not empty erreurs.prenom}"><span class="message-erreur"><c:out value="${erreurs.prenom}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.nom ? 'a-erreur' : ''}">
              <label for="nom">Nom</label>
              <input class="saisie" type="text" id="nom" name="nom" autocomplete="family-name" value="<c:out value='${nom}'/>">
              <c:if test="${not empty erreurs.nom}"><span class="message-erreur"><c:out value="${erreurs.nom}"/></span></c:if>
            </div>
          </div>
          <div class="champ ${not empty erreurs.email ? 'a-erreur' : ''}">
            <label for="email">Email</label>
            <input class="saisie" type="email" id="email" name="email" autocomplete="email" value="<c:out value='${email}'/>">
            <c:if test="${not empty erreurs.email}"><span class="message-erreur"><c:out value="${erreurs.email}"/></span></c:if>
          </div>
        </div>
        <div class="carte-pied">
          <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/compte">Annuler</a>
          <button type="submit" class="bouton bouton-principal">Enregistrer</button>
        </div>
      </form>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
