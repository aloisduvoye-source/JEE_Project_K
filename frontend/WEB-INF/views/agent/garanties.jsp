<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Garanties · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div class="entete-page">
        <div>
          <h1>Garanties</h1>
          <p class="sous-titre">Les couvertures que les types d'assurance peuvent proposer : responsabilité civile, vol, incendie…</p>
        </div>
        <div class="actions">
          <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/agent/garanties/nouvelle">
            <svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg>Nouvelle garantie
          </a>
        </div>
      </div>

      <%-- Confirmations (paramètre ajouté à l'adresse par la servlet après la redirection) --%>
      <c:choose>
        <c:when test="${not empty param.cree}"><c:set var="confirmation" value="La garantie a été créée."/><c:set var="parametre" value="cree"/></c:when>
        <c:when test="${not empty param.modifie}"><c:set var="confirmation" value="La garantie a été modifiée."/><c:set var="parametre" value="modifie"/></c:when>
        <c:when test="${not empty param.supprime}"><c:set var="confirmation" value="La garantie a été supprimée."/><c:set var="parametre" value="supprime"/></c:when>
      </c:choose>
      <c:if test="${not empty confirmation}">
        <div class="alerte ton-succes" role="status" data-nettoyer-url="${parametre}">
          <svg class="icone" aria-hidden="true"><use href="#i-succes"/></svg>
          <div class="alerte-texte"><c:out value="${confirmation}"/></div>
        </div>
      </c:if>
      <%-- Erreur enregistrée avant la redirection (MessageFlash), affichée une seule fois --%>
      <c:if test="${not empty sessionScope.messageErreur}">
        <div class="alerte ton-danger" role="alert">
          <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
          <div class="alerte-texte"><c:out value="${sessionScope.messageErreur}"/></div>
        </div>
        <c:remove var="messageErreur" scope="session"/>
      </c:if>

      <c:choose>
        <c:when test="${empty garanties}">
          <div class="carte vide">
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-bouclier-ok"/></svg></span>
            <h2>Aucune garantie</h2>
            <p>Créez une première garantie pour pouvoir la proposer dans un type d'assurance.</p>
            <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/agent/garanties/nouvelle">
              <svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg>Nouvelle garantie
            </a>
          </div>
        </c:when>
        <c:otherwise>
          <div>
            <div class="barre-outils avec-js" data-filtres="#tableau-garanties">
              <label class="recherche">
                <span class="visuellement-cache">Rechercher une garantie</span>
                <svg class="icone" aria-hidden="true"><use href="#i-recherche"/></svg>
                <input class="saisie" type="search" placeholder="Nom, description…" data-filtre-texte>
              </label>
              <span class="compteur" data-filtre-compteur></span>
            </div>

            <div class="tableau-conteneur">
              <table class="tableau" id="tableau-garanties">
                <thead>
                  <tr>
                    <th>Garantie</th>
                    <th class="a-droite">Plafond</th>
                    <th class="a-droite">Franchise</th>
                    <th class="a-droite">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="garantie" items="${garanties}">
                    <tr data-filtre-element>
                      <td>
                        <div class="cellule-principale"><c:out value="${garantie.nom}"/></div>
                        <c:if test="${not empty garantie.description}">
                          <div class="cellule-secondaire"><c:out value="${garantie.description}"/></div>
                        </c:if>
                      </td>
                      <%-- Plafond et franchise sont facultatifs : la base les accepte vides --%>
                      <td class="a-droite nombre">
                        <c:choose>
                          <c:when test="${garantie.plafond != null}"><app:euros valeur="${garantie.plafond}"/></c:when>
                          <c:otherwise><span class="texte-doux">Aucun</span></c:otherwise>
                        </c:choose>
                      </td>
                      <td class="a-droite nombre">
                        <c:choose>
                          <c:when test="${garantie.franchise != null}"><app:euros valeur="${garantie.franchise}"/></c:when>
                          <c:otherwise><span class="texte-doux">Aucune</span></c:otherwise>
                        </c:choose>
                      </td>
                      <td>
                        <div class="cellule-actions">
                          <a class="bouton bouton-petit bouton-fantome" title="Modifier"
                             href="${pageContext.request.contextPath}/agent/garanties/modifier?id=${garantie.id}">
                            <svg class="icone" aria-hidden="true"><use href="#i-crayon"/></svg>Modifier
                          </a>
                          <%-- Le service refuse la suppression d'une garantie souscrite par un contrat :
                               le message s'affiche alors en haut de cette liste --%>
                          <form method="post" action="${pageContext.request.contextPath}/agent/garanties/supprimer"
                                data-confirmer="Supprimer « <c:out value='${garantie.nom}'/> » ?"
                                data-confirmer-texte="La suppression est définitive. Elle est refusée si un contrat a souscrit cette garantie."
                                data-confirmer-bouton="Supprimer">
                            <input type="hidden" name="id" value="${garantie.id}">
                            <button type="submit" class="bouton bouton-petit bouton-danger-doux" title="Supprimer" aria-label="Supprimer">
                              <svg class="icone" aria-hidden="true"><use href="#i-poubelle"/></svg>
                            </button>
                          </form>
                        </div>
                      </td>
                    </tr>
                  </c:forEach>
                  <tr class="ligne-message" data-filtre-vide hidden>
                    <td colspan="4">Aucune garantie ne correspond à la recherche.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
