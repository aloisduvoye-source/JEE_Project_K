<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Contrats · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div class="entete-page">
        <div>
          <h1>Contrats</h1>
          <p class="sous-titre">${fn:length(contrats)} contrat${fn:length(contrats) > 1 ? 's' : ''}${recherche ? ' trouvé' : ''}${recherche and fn:length(contrats) > 1 ? 's' : ''}${recherche ? '' : ' au total'}.</p>
        </div>
        <%-- Seul un agent crée des contrats : l'admin consulte cette page sans le bouton --%>
        <c:if test="${sessionScope.utilisateur.role == 'AGENT'}">
          <div class="actions">
            <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/agent/contrats/nouveau">
              <svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg>Nouveau contrat
            </a>
          </div>
        </c:if>
      </div>

      <c:if test="${not empty param.cree}">
        <div class="alerte ton-succes" role="status" data-nettoyer-url="cree">
          <svg class="icone" aria-hidden="true"><use href="#i-succes"/></svg>
          <div class="alerte-texte">
            <strong>Le contrat n° <c:out value="${param.cree}"/> a été créé.</strong>
            Il deviendra actif à sa date de début s'il est payé.
          </div>
        </div>
      </c:if>
      <%-- Erreur enregistrée avant la redirection (MessageFlash), par exemple un contrat introuvable --%>
      <c:if test="${not empty sessionScope.messageErreur}">
        <div class="alerte ton-danger" role="alert">
          <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
          <div class="alerte-texte"><c:out value="${sessionScope.messageErreur}"/></div>
        </div>
        <c:remove var="messageErreur" scope="session"/>
      </c:if>

      <%-- Recherche, filtres et tri faits par le serveur, en GET : l'adresse peut être gardée ou partagée --%>
      <form class="barre-outils" method="get" action="${pageContext.request.contextPath}/agent/contrats" role="search">
        <label class="recherche">
          <span class="visuellement-cache">Rechercher un contrat</span>
          <svg class="icone" aria-hidden="true"><use href="#i-recherche"/></svg>
          <input class="saisie" type="search" name="q" maxlength="100" placeholder="N° de contrat, nom, email…" value="<c:out value='${q}'/>">
        </label>
        <%@ include file="/WEB-INF/views/common/contrats-filtres.jspf" %>
        <button type="submit" class="bouton bouton-principal">Rechercher</button>
        <c:if test="${recherche}">
          <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/agent/contrats">Effacer</a>
        </c:if>
      </form>

      <c:choose>
        <c:when test="${empty contrats and recherche}">
          <div class="carte vide">
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-recherche"/></svg></span>
            <h2>Aucun contrat trouvé</h2>
            <p>Aucun contrat ne correspond à la recherche.</p>
            <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/agent/contrats">Effacer la recherche</a>
          </div>
        </c:when>
        <c:when test="${empty contrats}">
          <div class="carte vide">
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg></span>
            <h2>Aucun contrat pour l'instant</h2>
            <p>Les contrats créés pour les clients apparaîtront ici.</p>
            <c:if test="${sessionScope.utilisateur.role == 'AGENT'}">
              <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/agent/contrats/nouveau">
                <svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg>Créer le premier contrat
              </a>
            </c:if>
          </div>
        </c:when>
        <c:otherwise>
          <div>
            <div class="tableau-conteneur">
              <table class="tableau" id="tableau-contrats">
                <thead>
                  <tr>
                    <th>N°</th>
                    <th>Client</th>
                    <th>Type d'assurance</th>
                    <th>Période</th>
                    <th class="a-droite">Prime annuelle</th>
                    <th>Statut</th>
                    <th><span class="visuellement-cache">Actions</span></th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="ct" items="${contrats}">
                    <c:set var="titulaire" value="${ct.client.utilisateur}"/>
                    <tr>
                      <td class="nombre"><a class="cellule-principale" href="${pageContext.request.contextPath}/agent/contrats/detail?id=${ct.id}">#<c:out value="${ct.id}"/></a></td>
                      <td>
                        <div class="cellule-personne">
                          <span class="avatar"><c:out value="${fn:toUpperCase(fn:substring(titulaire.prenom, 0, 1))}${fn:toUpperCase(fn:substring(titulaire.nom, 0, 1))}"/></span>
                          <div>
                            <div class="cellule-principale"><c:out value="${titulaire.prenom} ${titulaire.nom}"/></div>
                            <div class="cellule-secondaire"><c:out value="${titulaire.email}"/></div>
                          </div>
                        </div>
                      </td>
                      <td><c:out value="${ct.typeAssurance.nom}"/></td>
                      <td class="nombre"><app:date valeur="${ct.dateDebut}"/> → <app:date valeur="${ct.dateFin}"/></td>
                      <td class="a-droite nombre cellule-principale"><app:euros valeur="${ct.prime}"/></td>
                      <td><app:statutContrat valeur="${ct.statut}"/></td>
                      <td class="cellule-actions">
                        <a class="bouton bouton-petit bouton-fantome" href="${pageContext.request.contextPath}/agent/contrats/detail?id=${ct.id}">
                          Voir<svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg>
                        </a>
                      </td>
                    </tr>
                  </c:forEach>
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
