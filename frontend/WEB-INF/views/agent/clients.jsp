<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Clients · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div class="entete-page">
        <div>
          <h1>Clients</h1>
          <p class="sous-titre">
            <c:choose>
              <c:when test="${not empty q}">${fn:length(clients)} client${fn:length(clients) > 1 ? 's' : ''} pour « <c:out value="${q}"/> ».</c:when>
              <c:otherwise>${fn:length(clients)} client${fn:length(clients) > 1 ? 's' : ''} au total.</c:otherwise>
            </c:choose>
          </p>
        </div>
      </div>

      <%-- Recherche côté serveur (ClientDao.rechercher) : fonctionne aussi sans JavaScript --%>
      <form class="barre-outils" method="get" action="${pageContext.request.contextPath}/agent/clients" role="search">
        <label class="recherche ${not empty erreurs.q ? 'a-erreur' : ''}">
          <span class="visuellement-cache">Rechercher un client (nom, prénom ou email)</span>
          <svg class="icone" aria-hidden="true"><use href="#i-recherche"/></svg>
          <input class="saisie" type="search" id="q" name="q" value="<c:out value='${q}'/>" placeholder="Nom, prénom ou email…">
        </label>
        <button type="submit" class="bouton bouton-principal">Rechercher</button>
        <c:if test="${not empty q}">
          <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/agent/clients">
            <svg class="icone" aria-hidden="true"><use href="#i-fermer"/></svg>Effacer
          </a>
        </c:if>
      </form>
      <c:if test="${not empty erreurs.q}">
        <div class="alerte ton-danger" role="alert">
          <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
          <div class="alerte-texte"><c:out value="${erreurs.q}"/></div>
        </div>
      </c:if>

      <c:choose>
        <c:when test="${empty clients}">
          <div class="carte vide">
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-utilisateurs"/></svg></span>
            <c:choose>
              <c:when test="${not empty q}">
                <h2>Aucun résultat</h2>
                <p>Aucun client ne correspond à « <c:out value="${q}"/> ».</p>
                <a class="bouton" href="${pageContext.request.contextPath}/agent/clients">Afficher tous les clients</a>
              </c:when>
              <c:otherwise>
                <h2>Aucun client pour l'instant</h2>
                <p>Les clients apparaîtront ici dès qu'ils auront créé leur compte.</p>
              </c:otherwise>
            </c:choose>
          </div>
        </c:when>
        <c:otherwise>
          <div class="tableau-conteneur">
            <table class="tableau">
              <thead>
                <tr>
                  <th>Client</th>
                  <th>Téléphone</th>
                  <th>Adresse</th>
                  <th>Compte</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="client" items="${clients}">
                  <c:set var="u" value="${client.utilisateur}"/>
                  <tr class="${u.actif ? '' : 'ligne-attenuee'}">
                    <td>
                      <div class="cellule-personne">
                        <span class="avatar"><c:out value="${fn:toUpperCase(fn:substring(u.prenom, 0, 1))}${fn:toUpperCase(fn:substring(u.nom, 0, 1))}"/></span>
                        <div>
                          <div class="cellule-principale"><c:out value="${u.prenom} ${u.nom}"/></div>
                          <div class="cellule-secondaire"><c:out value="${u.email}"/></div>
                        </div>
                      </div>
                    </td>
                    <td class="nombre"><c:out value="${empty client.telephone ? '—' : client.telephone}"/></td>
                    <td><c:out value="${empty client.adresse ? '—' : client.adresse}"/></td>
                    <td>
                      <c:choose>
                        <c:when test="${u.actif}"><span class="badge badge-point ton-succes">Actif</span></c:when>
                        <c:otherwise><span class="badge badge-point ton-neutre">Désactivé</span></c:otherwise>
                      </c:choose>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
