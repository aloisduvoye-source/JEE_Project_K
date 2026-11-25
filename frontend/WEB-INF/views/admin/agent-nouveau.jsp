<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Nouvel agent · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur conteneur-etroit apparition">
      <div>
        <a class="retour" href="${pageContext.request.contextPath}/admin/utilisateurs">
          <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Utilisateurs
        </a>
        <div class="entete-page">
          <div>
            <h1>Nouvel agent</h1>
            <p class="sous-titre">L'agent pourra changer son mot de passe provisoire depuis « Mon compte ».</p>
          </div>
        </div>
      </div>

      <form class="carte" method="post" action="${pageContext.request.contextPath}/admin/utilisateurs/nouvel-agent" novalidate>
        <div class="carte-entete"><div><h2>Identité</h2><p>Les champs marqués d'un astérisque sont obligatoires.</p></div></div>
        <div class="carte-corps formulaire">
          <div class="champs-2">
            <div class="champ ${not empty erreurs.prenom ? 'a-erreur' : ''}">
              <label for="prenom">Prénom<span class="obligatoire">*</span></label>
              <input class="saisie" type="text" id="prenom" name="prenom" value="<c:out value='${prenom}'/>" autocomplete="off">
              <c:if test="${not empty erreurs.prenom}"><span class="message-erreur"><c:out value="${erreurs.prenom}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.nom ? 'a-erreur' : ''}">
              <label for="nom">Nom<span class="obligatoire">*</span></label>
              <input class="saisie" type="text" id="nom" name="nom" value="<c:out value='${nom}'/>" autocomplete="off">
              <c:if test="${not empty erreurs.nom}"><span class="message-erreur"><c:out value="${erreurs.nom}"/></span></c:if>
            </div>
          </div>
          <div class="champ ${not empty erreurs.email ? 'a-erreur' : ''}">
              <label for="email">Email<span class="obligatoire">*</span></label>
              <input class="saisie" type="email" id="email" name="email" value="<c:out value='${email}'/>" autocomplete="off">
              <c:if test="${not empty erreurs.email}"><span class="message-erreur"><c:out value="${erreurs.email}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.identifiant ? 'a-erreur' : ''}">
              <label for="identifiant">Identifiant<span class="obligatoire">*</span></label>
              <input class="saisie" type="text" id="identifiant" name="identifiant" value="<c:out value='${identifiant}'/>" autocomplete="off">
              <span class="aide">Utilisé par l'agent pour se connecter.</span>
              <c:if test="${not empty erreurs.identifiant}"><span class="message-erreur"><c:out value="${erreurs.identifiant}"/></span></c:if>
            </div>
          <div class="champs-2">
            <div class="champ ${not empty erreurs.motDePasse ? 'a-erreur' : ''}">
              <label for="motDePasse">Mot de passe provisoire<span class="obligatoire">*</span></label>
              <div class="saisie-groupe">
                <input class="saisie" type="password" id="motDePasse" name="motDePasse" autocomplete="new-password" data-force>
                <button type="button" class="saisie-bouton" data-voir-mdp aria-label="Afficher le mot de passe">
                  <svg class="icone" aria-hidden="true"><use href="#i-oeil"/></svg>
                </button>
              </div>
              <span class="aide">8 caractères minimum.</span>
              <c:if test="${not empty erreurs.motDePasse}"><span class="message-erreur"><c:out value="${erreurs.motDePasse}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.confirmation ? 'a-erreur' : ''}">
              <label for="confirmation">Confirmer le mot de passe<span class="obligatoire">*</span></label>
              <input class="saisie" type="password" id="confirmation" name="confirmation" autocomplete="new-password" data-identique="#motDePasse">
              <c:if test="${not empty erreurs.confirmation}"><span class="message-erreur"><c:out value="${erreurs.confirmation}"/></span></c:if>
            </div>
          </div>
        </div>
        <div class="carte-pied">
          <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/admin/utilisateurs">Annuler</a>
          <button type="submit" class="bouton bouton-principal">
            <svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg>Créer le compte agent
          </button>
        </div>
      </form>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
