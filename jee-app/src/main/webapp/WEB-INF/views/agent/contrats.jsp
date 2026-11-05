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
          <p class="sous-titre">${fn:length(contrats)} contrat${fn:length(contrats) > 1 ? 's' : ''} au total.</p>
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
            Il est en attente d'activation.
          </div>
        </div>
      </c:if>

      <c:choose>
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
            <div class="barre-outils avec-js" data-filtres="#tableau-contrats">
              <label class="recherche">
                <span class="visuellement-cache">Rechercher un contrat</span>
                <svg class="icone" aria-hidden="true"><use href="#i-recherche"/></svg>
                <input class="saisie" type="search" placeholder="Client, type, numéro…" data-filtre-texte>
              </label>
              <div class="puces" role="group" aria-label="Filtrer par statut">
                <button type="button" class="puce" data-filtre-valeur="" aria-pressed="true">Tous</button>
                <button type="button" class="puce" data-filtre-valeur="EN_ATTENTE" aria-pressed="false">En attente</button>
                <button type="button" class="puce" data-filtre-valeur="ACTIF" aria-pressed="false">Actifs</button>
                <button type="button" class="puce" data-filtre-valeur="SUSPENDU" aria-pressed="false">Suspendus</button>
                <button type="button" class="puce" data-filtre-valeur="EXPIRE" aria-pressed="false">Expirés</button>
                <button type="button" class="puce" data-filtre-valeur="RESILIE" aria-pressed="false">Résiliés</button>
              </div>
              <span class="compteur" data-filtre-compteur></span>
            </div>

            <div class="tableau-conteneur">
              <table class="tableau" id="tableau-contrats">
                <thead>
                  <tr>
                    <th>N°</th>
                    <th>Client</th>
                    <th>Type d'assurance</th>
                    <th>Période</th>
                    <th class="a-droite">Prime</th>
                    <th>Statut</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="ct" items="${contrats}">
                    <c:set var="titulaire" value="${ct.client.utilisateur}"/>
                    <tr data-filtre-element data-filtre-cle="${ct.statut}">
                      <td class="texte-doux nombre">#<c:out value="${ct.id}"/></td>
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
                    </tr>
                  </c:forEach>
                  <tr class="ligne-message" data-filtre-vide hidden>
                    <td colspan="6">Aucun contrat ne correspond à la recherche.</td>
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
