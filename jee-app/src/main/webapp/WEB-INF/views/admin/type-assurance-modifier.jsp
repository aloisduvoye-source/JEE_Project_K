<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Modifier un type d'assurance · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <c:set var="action" value="${pageContext.request.contextPath}/admin/types-assurance/modifier"/>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div>
        <a class="retour" href="${pageContext.request.contextPath}/admin/types-assurance">
          <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Types d'assurance
        </a>
        <div class="entete-page">
          <div>
            <h1><c:out value="${type.nom}"/></h1>
            <p class="sous-titre">
              <c:choose>
                <c:when test="${type.actif}"><span class="badge badge-point ton-succes">Actif</span></c:when>
                <c:otherwise><span class="badge badge-point ton-neutre">Archivé</span></c:otherwise>
              </c:choose>
              Les modifications ne s'appliquent qu'aux nouveaux contrats.
            </p>
          </div>
        </div>
      </div>

      <c:if test="${not empty erreurs.global}">
        <div class="alerte ton-danger" role="alert">
          <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
          <div class="alerte-texte"><c:out value="${erreurs.global}"/></div>
        </div>
      </c:if>
      <c:if test="${not empty erreurs.garantie}">
        <div class="alerte ton-danger" role="alert">
          <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
          <div class="alerte-texte"><c:out value="${erreurs.garantie}"/></div>
        </div>
      </c:if>
      <c:if test="${not empty param.garanties}">
        <div class="alerte ton-succes" role="status" data-nettoyer-url="garanties">
          <svg class="icone" aria-hidden="true"><use href="#i-succes"/></svg>
          <div class="alerte-texte">Les garanties proposées ont été mises à jour.</div>
        </div>
      </c:if>

      <div class="grille grille-2">
        <%-- Informations du type --%>
        <form class="carte" method="post" action="${action}" novalidate>
          <input type="hidden" name="id" value="${type.id}">
          <input type="hidden" name="action" value="infos">
          <div class="carte-entete"><div><h2>Informations</h2><p>Nom, description et prix de base.</p></div></div>
          <div class="carte-corps formulaire">
            <div class="champ ${not empty erreurs.nom ? 'a-erreur' : ''}">
              <label for="nom">Nom</label>
              <input class="saisie" type="text" id="nom" name="nom" maxlength="100" value="<c:out value='${nom}'/>">
              <c:if test="${not empty erreurs.nom}"><span class="message-erreur"><c:out value="${erreurs.nom}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.description ? 'a-erreur' : ''}">
              <label for="description">Description</label>
              <textarea class="saisie" id="description" name="description" rows="4" maxlength="1000" data-compteur><c:out value="${description}"/></textarea>
              <c:if test="${not empty erreurs.description}"><span class="message-erreur"><c:out value="${erreurs.description}"/></span></c:if>
            </div>
            <div class="champ ${not empty erreurs.prixBase ? 'a-erreur' : ''}">
              <label for="prixBase">Prix de base</label>
              <div class="saisie-groupe">
                <input class="saisie" type="text" inputmode="decimal" id="prixBase" name="prixBase" value="<c:out value='${prixBase}'/>">
                <span class="saisie-suffixe">€</span>
              </div>
              <span class="aide">Les contrats existants gardent leur prime.</span>
              <c:if test="${not empty erreurs.prixBase}"><span class="message-erreur"><c:out value="${erreurs.prixBase}"/></span></c:if>
            </div>
          </div>
          <div class="carte-pied">
            <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/admin/types-assurance">Annuler</a>
            <button type="submit" class="bouton bouton-principal">Enregistrer</button>
          </div>
        </form>

        <%-- Garanties proposées : chaque bouton envoie son propre formulaire --%>
        <section class="carte">
          <div class="carte-entete">
            <div>
              <h2>Garanties proposées</h2>
              <p>Les contrats existants gardent leurs garanties.</p>
            </div>
            <span class="badge ton-primaire">${fn:length(garantiesProposees)}</span>
          </div>
          <ul class="liste-lignes">
            <c:forEach var="g" items="${garantiesProposees}">
              <li>
                <div>
                  <div class="cellule-principale"><c:out value="${g.nom}"/></div>
                  <div class="detail">Plafond <app:euros valeur="${g.plafond}" vide="aucun"/> · franchise <app:euros valeur="${g.franchise}" vide="aucune"/></div>
                </div>
                <%-- Un type doit garder au moins une garantie : pas de bouton sur la dernière --%>
                <c:choose>
                  <c:when test="${fn:length(garantiesProposees) > 1}">
                    <form method="post" action="${action}">
                      <input type="hidden" name="id" value="${type.id}">
                      <input type="hidden" name="action" value="retirer">
                      <input type="hidden" name="garantie" value="${g.id}">
                      <button type="submit" class="bouton bouton-petit bouton-danger-doux">
                        <svg class="icone" aria-hidden="true"><use href="#i-fermer"/></svg>Retirer
                      </button>
                    </form>
                  </c:when>
                  <c:otherwise><span class="badge ton-neutre" title="Un type doit proposer au moins une garantie">Dernière garantie</span></c:otherwise>
                </c:choose>
              </li>
            </c:forEach>
          </ul>
          <div class="carte-pied" style="justify-content: stretch">
            <c:choose>
              <c:when test="${not empty garantiesAutres}">
                <form method="post" action="${action}" style="display:flex;gap:10px;width:100%;flex-wrap:wrap">
                  <input type="hidden" name="id" value="${type.id}">
                  <input type="hidden" name="action" value="ajouter">
                  <label for="garantie" class="visuellement-cache">Garantie à ajouter</label>
                  <select class="saisie" id="garantie" name="garantie" style="flex:1;min-width:180px">
                    <c:forEach var="g" items="${garantiesAutres}">
                      <option value="${g.id}"><c:out value="${g.nom}"/></option>
                    </c:forEach>
                  </select>
                  <button type="submit" class="bouton">
                    <svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg>Ajouter
                  </button>
                </form>
              </c:when>
              <c:otherwise>
                <p class="aide">Toutes les garanties existantes sont déjà proposées.</p>
              </c:otherwise>
            </c:choose>
          </div>
        </section>
      </div>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
