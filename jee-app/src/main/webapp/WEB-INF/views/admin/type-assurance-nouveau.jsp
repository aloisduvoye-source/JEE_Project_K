<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Nouveau type d'assurance · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur conteneur-etroit apparition">
      <div>
        <a class="retour" href="${pageContext.request.contextPath}/admin/types-assurance">
          <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Types d'assurance
        </a>
        <div class="entete-page">
          <div>
            <h1>Nouveau type d'assurance</h1>
            <p class="sous-titre">Il sera proposé tout de suite pour les nouveaux contrats.</p>
          </div>
        </div>
      </div>

      <c:if test="${not empty erreurs.global}">
        <div class="alerte ton-danger" role="alert">
          <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
          <div class="alerte-texte"><c:out value="${erreurs.global}"/></div>
        </div>
      </c:if>

      <form class="carte" method="post" action="${pageContext.request.contextPath}/admin/types-assurance/nouveau" novalidate>
        <div class="carte-corps formulaire">
          <div class="champs-2">
            <div class="champ ${not empty erreurs.nom ? 'a-erreur' : ''}">
              <label for="nom">Nom<span class="obligatoire">*</span></label>
              <input class="saisie" type="text" id="nom" name="nom" maxlength="100" placeholder="Assurance animaux"
                     value="<c:out value='${nom}'/>">
              <c:if test="${not empty erreurs.nom}"><span class="message-erreur"><c:out value="${erreurs.nom}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.prixBase ? 'a-erreur' : ''}">
              <label for="prixBase">Prix de base<span class="obligatoire">*</span></label>
              <div class="saisie-groupe">
                <input class="saisie" type="text" inputmode="decimal" id="prixBase" name="prixBase" placeholder="250,00"
                       value="<c:out value='${prixBase}'/>">
                <span class="saisie-suffixe">€</span>
              </div>
              <span class="aide">Prime minimale d'un contrat de ce type.</span>
              <c:if test="${not empty erreurs.prixBase}"><span class="message-erreur"><c:out value="${erreurs.prixBase}"/></span></c:if>
            </div>
          </div>

          <div class="champ ${not empty erreurs.description ? 'a-erreur' : ''}">
            <label for="description">Description</label>
            <textarea class="saisie" id="description" name="description" rows="3" maxlength="1000" data-compteur
                      placeholder="Ce que couvre ce type d'assurance."><c:out value="${description}"/></textarea>
            <c:if test="${not empty erreurs.description}"><span class="message-erreur"><c:out value="${erreurs.description}"/></span></c:if>
          </div>

          <fieldset class="${not empty erreurs.garanties ? 'a-erreur' : ''}">
            <legend class="libelle">Garanties proposées<span class="obligatoire">*</span></legend>
            <c:choose>
              <c:when test="${empty garanties}">
                <p class="texte-doux">Aucune garantie n'existe pour l'instant.</p>
              </c:when>
              <c:otherwise>
                <div class="choix-cases">
                  <c:forEach var="g" items="${garanties}">
                    <label class="case-carte" for="garantie${g.id}">
                      <input type="checkbox" id="garantie${g.id}" name="garanties" value="${g.id}"
                             ${garantiesCochees.contains(g.id) ? 'checked' : ''}>
                      <span>
                        <span class="case-titre"><c:out value="${g.nom}"/></span>
                        <span class="case-detail">Plafond <app:euros valeur="${g.plafond}"/> · franchise <app:euros valeur="${g.franchise}"/></span>
                      </span>
                    </label>
                  </c:forEach>
                </div>
              </c:otherwise>
            </c:choose>
            <c:if test="${not empty erreurs.garanties}"><p class="message-erreur" style="margin-top:10px"><c:out value="${erreurs.garanties}"/></p></c:if>
          </fieldset>
        </div>
        <div class="carte-pied">
          <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/admin/types-assurance">Annuler</a>
          <button type="submit" class="bouton bouton-principal">Créer le type d'assurance</button>
        </div>
      </form>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
