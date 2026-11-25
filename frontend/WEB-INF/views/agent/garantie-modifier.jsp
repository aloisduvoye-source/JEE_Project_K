<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Modifier une garantie · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur conteneur-etroit apparition">
      <div>
        <a class="retour" href="${pageContext.request.contextPath}/agent/garanties">
          <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Garanties
        </a>
        <div class="entete-page">
          <div>
            <h1><c:out value="${garantie.nom}"/></h1>
            <%-- Les contrats ne gardent que l'identifiant de la garantie (table contrat_garantie) :
                 contrairement à la prime d'un contrat, les montants modifiés ici s'appliquent partout --%>
            <p class="sous-titre">Les modifications s'appliquent aussi aux contrats qui ont déjà souscrit cette garantie.</p>
          </div>
        </div>
      </div>

      <c:if test="${not empty erreurs.global}">
        <div class="alerte ton-danger" role="alert">
          <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
          <div class="alerte-texte"><c:out value="${erreurs.global}"/></div>
        </div>
      </c:if>

      <form class="carte" method="post" action="${pageContext.request.contextPath}/agent/garanties/modifier" novalidate>
        <input type="hidden" name="id" value="${garantie.id}">
        <div class="carte-corps formulaire">
          <div class="champ ${not empty erreurs.nom ? 'a-erreur' : ''}">
            <label for="nom">Nom<span class="obligatoire">*</span></label>
            <input class="saisie" type="text" id="nom" name="nom" maxlength="100" value="<c:out value='${nom}'/>">
            <c:if test="${not empty erreurs.nom}"><span class="message-erreur"><c:out value="${erreurs.nom}"/></span></c:if>
          </div>

          <div class="champ ${not empty erreurs.description ? 'a-erreur' : ''}">
            <label for="description">Description</label>
            <textarea class="saisie" id="description" name="description" rows="4" maxlength="1000" data-compteur><c:out value="${description}"/></textarea>
            <c:if test="${not empty erreurs.description}"><span class="message-erreur"><c:out value="${erreurs.description}"/></span></c:if>
          </div>

          <div class="champs-2">
            <div class="champ ${not empty erreurs.plafond ? 'a-erreur' : ''}">
              <label for="plafond">Plafond</label>
              <div class="saisie-groupe">
                <input class="saisie" type="text" inputmode="decimal" id="plafond" name="plafond" value="<c:out value='${plafond}'/>">
                <span class="saisie-suffixe">€</span>
              </div>
              <span class="aide">Montant maximum remboursé. Facultatif.</span>
              <c:if test="${not empty erreurs.plafond}"><span class="message-erreur"><c:out value="${erreurs.plafond}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.franchise ? 'a-erreur' : ''}">
              <label for="franchise">Franchise</label>
              <div class="saisie-groupe">
                <input class="saisie" type="text" inputmode="decimal" id="franchise" name="franchise" value="<c:out value='${franchise}'/>">
                <span class="saisie-suffixe">€</span>
              </div>
              <span class="aide">Montant restant à la charge du client. Facultatif.</span>
              <c:if test="${not empty erreurs.franchise}"><span class="message-erreur"><c:out value="${erreurs.franchise}"/></span></c:if>
            </div>
          </div>
        </div>
        <div class="carte-pied">
          <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/agent/garanties">Annuler</a>
          <button type="submit" class="bouton bouton-principal">Enregistrer</button>
        </div>
      </form>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
