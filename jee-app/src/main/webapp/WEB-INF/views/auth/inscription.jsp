<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Créer un compte · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="auth">
    <div class="auth-carte auth-carte-large apparition">
      <div class="auth-entete">
        <span class="logo-icone"><svg class="icone" aria-hidden="true"><use href="#i-utilisateur"/></svg></span>
        <h1>Créer un compte</h1>
        <p>Quelques informations pour ouvrir votre espace client.</p>
      </div>

      <form class="carte" method="post" action="${pageContext.request.contextPath}/inscription" novalidate>
        <div class="carte-corps formulaire">
          <c:if test="${not empty erreurs}">
            <div class="alerte ton-danger" role="alert">
              <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
              <div class="alerte-texte">Certains champs sont à corriger.</div>
            </div>
          </c:if>

          <div class="champ ${not empty erreurs.identifiant ? 'a-erreur' : ''}">
            <label for="identifiant">Identifiant<span class="obligatoire">*</span></label>
            <input class="saisie" type="text" id="identifiant" name="identifiant" autocomplete="username"
                   value="<c:out value='${identifiant}'/>">
            <c:if test="${not empty erreurs.identifiant}"><span class="message-erreur"><c:out value="${erreurs.identifiant}"/></span></c:if>
          </div>

          <div class="champs-2">
            <div class="champ ${not empty erreurs.prenom ? 'a-erreur' : ''}">
              <label for="prenom">Prénom<span class="obligatoire">*</span></label>
              <input class="saisie" type="text" id="prenom" name="prenom" autocomplete="given-name" value="<c:out value='${prenom}'/>">
              <c:if test="${not empty erreurs.prenom}"><span class="message-erreur"><c:out value="${erreurs.prenom}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.nom ? 'a-erreur' : ''}">
              <label for="nom">Nom<span class="obligatoire">*</span></label>
              <input class="saisie" type="text" id="nom" name="nom" autocomplete="family-name" value="<c:out value='${nom}'/>">
              <c:if test="${not empty erreurs.nom}"><span class="message-erreur"><c:out value="${erreurs.nom}"/></span></c:if>
            </div>
          </div>

          <div class="champs-2">
            <div class="champ ${not empty erreurs.email ? 'a-erreur' : ''}">
              <label for="email">Email<span class="obligatoire">*</span></label>
              <input class="saisie" type="email" id="email" name="email" autocomplete="email" value="<c:out value='${email}'/>">
              <c:if test="${not empty erreurs.email}"><span class="message-erreur"><c:out value="${erreurs.email}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.telephone ? 'a-erreur' : ''}">
              <label for="telephone">Téléphone</label>
              <input class="saisie" type="tel" id="telephone" name="telephone" autocomplete="tel"
                     placeholder="06 12 34 56 78" value="<c:out value='${telephone}'/>">
              <c:if test="${not empty erreurs.telephone}"><span class="message-erreur"><c:out value="${erreurs.telephone}"/></span></c:if>
            </div>
          </div>

          <div class="champs-2">
            <div class="champ ${not empty erreurs.adresse ? 'a-erreur' : ''}">
              <label for="adresse">Adresse</label>
              <input class="saisie" type="text" id="adresse" name="adresse" autocomplete="street-address" value="<c:out value='${adresse}'/>">
              <c:if test="${not empty erreurs.adresse}"><span class="message-erreur"><c:out value="${erreurs.adresse}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.dateNaissance ? 'a-erreur' : ''}">
              <label for="dateNaissance">Date de naissance</label>
              <input class="saisie" type="date" id="dateNaissance" name="dateNaissance" autocomplete="bday" value="<c:out value='${dateNaissance}'/>">
              <c:if test="${not empty erreurs.dateNaissance}"><span class="message-erreur"><c:out value="${erreurs.dateNaissance}"/></span></c:if>
            </div>
          </div>

          <div class="champs-2">
            <div class="champ ${not empty erreurs.motDePasse ? 'a-erreur' : ''}">
              <label for="motDePasse">Mot de passe<span class="obligatoire">*</span></label>
              <div class="saisie-groupe">
                <input class="saisie" type="password" id="motDePasse" name="motDePasse" autocomplete="new-password" data-force>
                <button type="button" class="saisie-bouton" data-voir-mdp aria-label="Afficher le mot de passe">
                  <svg class="icone" aria-hidden="true"><use href="#i-oeil"/></svg>
                </button>
              </div>
              <c:if test="${not empty erreurs.motDePasse}"><span class="message-erreur"><c:out value="${erreurs.motDePasse}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.confirmation ? 'a-erreur' : ''}">
              <label for="confirmation">Confirmer le mot de passe<span class="obligatoire">*</span></label>
              <input class="saisie" type="password" id="confirmation" name="confirmation" autocomplete="new-password" data-identique="#motDePasse">
              <c:if test="${not empty erreurs.confirmation}"><span class="message-erreur"><c:out value="${erreurs.confirmation}"/></span></c:if>
            </div>
          </div>
        </div>

        <div class="carte-pied" style="justify-content: space-between; align-items: center">
          <span class="aide"><span class="obligatoire">*</span> champs obligatoires</span>
          <button type="submit" class="bouton bouton-principal">Créer mon compte</button>
        </div>
      </form>

      <p class="auth-pied">
        Déjà un compte ? <a href="${pageContext.request.contextPath}/connexion">Se connecter</a>
      </p>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
