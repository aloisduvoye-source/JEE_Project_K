<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Types d'assurance · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div class="entete-page">
        <div>
          <h1>Types d'assurance</h1>
          <p class="sous-titre">Le catalogue proposé aux clients : un type archivé n'est plus proposé pour les nouveaux contrats.</p>
        </div>
        <div class="actions">
          <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/admin/types-assurance/nouveau">
            <svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg>Nouveau type
          </a>
        </div>
      </div>

      <%-- Confirmations (paramètre ajouté à l'adresse par la servlet après la redirection) --%>
      <c:choose>
        <c:when test="${not empty param.cree}"><c:set var="confirmation" value="Le type d'assurance a été créé."/><c:set var="parametre" value="cree"/></c:when>
        <c:when test="${not empty param.modifie}"><c:set var="confirmation" value="Le type d'assurance a été modifié."/><c:set var="parametre" value="modifie"/></c:when>
        <c:when test="${not empty param.supprime}"><c:set var="confirmation" value="Le type d'assurance a été supprimé."/><c:set var="parametre" value="supprime"/></c:when>
        <c:when test="${not empty param.archive}"><c:set var="confirmation" value="Le type d'assurance a été archivé : il n'est plus proposé pour les nouveaux contrats."/><c:set var="parametre" value="archive"/></c:when>
        <c:when test="${not empty param.reactive}"><c:set var="confirmation" value="Le type d'assurance a été réactivé."/><c:set var="parametre" value="reactive"/></c:when>
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
        <c:when test="${empty types}">
          <div class="carte vide">
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-couches"/></svg></span>
            <h2>Aucun type d'assurance</h2>
            <p>Créez un premier type d'assurance pour pouvoir proposer des contrats.</p>
            <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/admin/types-assurance/nouveau">
              <svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg>Nouveau type
            </a>
          </div>
        </c:when>
        <c:otherwise>
          <div>
            <div class="barre-outils avec-js" data-filtres="#tableau-types">
              <label class="recherche">
                <span class="visuellement-cache">Rechercher un type d'assurance</span>
                <svg class="icone" aria-hidden="true"><use href="#i-recherche"/></svg>
                <input class="saisie" type="search" placeholder="Nom, garantie…" data-filtre-texte>
              </label>
              <div class="puces" role="group" aria-label="Filtrer par état">
                <button type="button" class="puce" data-filtre-valeur="" aria-pressed="true">Tous</button>
                <button type="button" class="puce" data-filtre-valeur="actif" aria-pressed="false">Actifs</button>
                <button type="button" class="puce" data-filtre-valeur="archive" aria-pressed="false">Archivés</button>
              </div>
              <span class="compteur" data-filtre-compteur></span>
            </div>

            <div class="tableau-conteneur">
              <table class="tableau" id="tableau-types">
                <thead>
                  <tr>
                    <th>Type d'assurance</th>
                    <th class="a-droite">Prix de base</th>
                    <th>Garanties disponibles</th>
                    <th>État</th>
                    <th class="a-droite">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="type" items="${types}">
                    <tr class="${type.actif ? '' : 'ligne-attenuee'}" data-filtre-element data-filtre-cle="${type.actif ? 'actif' : 'archive'}">
                      <td>
                        <div class="cellule-principale"><c:out value="${type.nom}"/></div>
                        <c:if test="${not empty type.description}">
                          <div class="cellule-secondaire"><c:out value="${type.description}"/></div>
                        </c:if>
                      </td>
                      <td class="a-droite nombre cellule-principale"><app:euros valeur="${type.prixBase}"/></td>
                      <td>
                        <div class="etiquettes">
                          <c:forEach var="g" items="${type.garanties}">
                            <span class="etiquette"><c:out value="${g.nom}"/></span>
                          </c:forEach>
                          <c:if test="${empty type.garanties}"><span class="texte-doux">Aucune</span></c:if>
                        </div>
                      </td>
                      <td>
                        <c:choose>
                          <c:when test="${type.actif}"><span class="badge badge-point ton-succes">Actif</span></c:when>
                          <c:otherwise><span class="badge badge-point ton-neutre">Archivé</span></c:otherwise>
                        </c:choose>
                      </td>
                      <td>
                        <div class="cellule-actions">
                          <a class="bouton bouton-petit bouton-fantome" title="Modifier"
                             href="${pageContext.request.contextPath}/admin/types-assurance/modifier?id=${type.id}">
                            <svg class="icone" aria-hidden="true"><use href="#i-crayon"/></svg>Modifier
                          </a>
                          <%-- Archiver : alternative à la suppression pour un type utilisé par des contrats --%>
                          <form method="post" action="${pageContext.request.contextPath}/admin/types-assurance/${type.actif ? 'archiver' : 'reactiver'}">
                            <input type="hidden" name="id" value="${type.id}">
                            <button type="submit" class="bouton bouton-petit bouton-fantome">
                              <svg class="icone" aria-hidden="true"><use href="#i-${type.actif ? 'archive' : 'restaurer'}"/></svg>${type.actif ? 'Archiver' : 'Réactiver'}
                            </button>
                          </form>
                          <%-- Suppression proposée seulement pour un type jamais utilisé (créé par erreur, doublon...) :
                               un type utilisé par un contrat s'archive. Le service refuse de toute façon la suppression. --%>
                          <c:if test="${not typesUtilises.contains(type.id)}">
                            <form method="post" action="${pageContext.request.contextPath}/admin/types-assurance/supprimer"
                                  data-confirmer="Supprimer « <c:out value='${type.nom}'/> » ?"
                                  data-confirmer-texte="Ce type n'est utilisé par aucun contrat. La suppression est définitive."
                                  data-confirmer-bouton="Supprimer">
                              <input type="hidden" name="id" value="${type.id}">
                              <button type="submit" class="bouton bouton-petit bouton-danger-doux" title="Supprimer" aria-label="Supprimer">
                                <svg class="icone" aria-hidden="true"><use href="#i-poubelle"/></svg>
                              </button>
                            </form>
                          </c:if>
                        </div>
                      </td>
                    </tr>
                  </c:forEach>
                  <tr class="ligne-message" data-filtre-vide hidden>
                    <td colspan="5">Aucun type d'assurance ne correspond à la recherche.</td>
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
