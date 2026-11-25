<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Utilisateurs · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div class="entete-page">
        <div>
          <h1>Utilisateurs</h1>
          <p class="sous-titre">${fn:length(utilisateurs)} compte${fn:length(utilisateurs) > 1 ? 's' : ''}<c:if test="${not empty role}"> avec ce rôle</c:if>. Un compte désactivé ne peut plus se connecter, son historique est conservé.</p>
        </div>
        <div class="actions">
          <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/admin/utilisateurs/nouvel-agent">
            <svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg>Nouvel agent
          </a>
        </div>
      </div>

      <%-- Confirmations (paramètre ajouté à l'adresse par la servlet après la redirection) --%>
      <c:choose>
        <c:when test="${not empty param.agentCree}"><c:set var="confirmation" value="Le compte agent a été créé."/><c:set var="parametre" value="agentCree"/></c:when>
        <c:when test="${not empty param.desactive}"><c:set var="confirmation" value="Le compte a été désactivé : il ne peut plus se connecter, son historique est conservé."/><c:set var="parametre" value="desactive"/></c:when>
        <c:when test="${not empty param.reactive}"><c:set var="confirmation" value="Le compte a été réactivé."/><c:set var="parametre" value="reactive"/></c:when>
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

      <%-- Filtre par rôle côté serveur (UtilisateurDao.listerParRole) : de simples liens --%>
      <div class="barre-outils">
        <nav class="puces" aria-label="Filtrer par rôle">
          <a class="puce" href="${pageContext.request.contextPath}/admin/utilisateurs" ${empty role ? 'aria-current="page"' : ''}>Tous</a>
          <c:forEach var="r" items="${roles}">
            <a class="puce" href="${pageContext.request.contextPath}/admin/utilisateurs?role=${r}" ${r == role ? 'aria-current="page"' : ''}>${r == 'ADMIN' ? 'Administrateur' : (r == 'AGENT' ? 'Agents' : 'Clients')}</a>
          </c:forEach>
        </nav>
        <div class="recherche avec-js" data-filtres="#tableau-utilisateurs">
          <label>
            <span class="visuellement-cache">Rechercher dans la liste</span>
            <svg class="icone" aria-hidden="true"><use href="#i-recherche"/></svg>
            <input class="saisie" type="search" placeholder="Nom, email, identifiant…" data-filtre-texte>
          </label>
        </div>
      </div>

      <c:choose>
        <c:when test="${empty utilisateurs}">
          <div class="carte vide">
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-utilisateurs"/></svg></span>
            <h2>Aucun utilisateur à afficher</h2>
            <p>Aucun compte n'a ce rôle pour l'instant.</p>
          </div>
        </c:when>
        <c:otherwise>
          <div class="tableau-conteneur">
            <table class="tableau" id="tableau-utilisateurs">
              <thead>
                <tr>
                  <th>Utilisateur</th>
                  <th>Identifiant</th>
                  <th>Rôle</th>
                  <th>Compte</th>
                  <th>Inscription</th>
                  <th class="a-droite">Actions</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="u" items="${utilisateurs}">
                  <tr class="${u.actif ? '' : 'ligne-attenuee'}" data-filtre-element>
                    <td>
                      <div class="cellule-personne">
                        <span class="avatar"><c:out value="${fn:toUpperCase(fn:substring(u.prenom, 0, 1))}${fn:toUpperCase(fn:substring(u.nom, 0, 1))}"/></span>
                        <div>
                          <div class="cellule-principale"><c:out value="${u.prenom} ${u.nom}"/></div>
                          <div class="cellule-secondaire"><c:out value="${u.email}"/></div>
                        </div>
                      </div>
                    </td>
                    <td class="texte-doux"><c:out value="${u.identifiant}"/></td>
                    <td>
                      <c:choose>
                        <c:when test="${u.role == 'ADMIN'}"><span class="badge ton-violet">Administrateur</span></c:when>
                        <c:when test="${u.role == 'AGENT'}"><span class="badge ton-info">Agent</span></c:when>
                        <c:otherwise><span class="badge ton-primaire">Client</span></c:otherwise>
                      </c:choose>
                    </td>
                    <td>
                      <c:choose>
                        <c:when test="${u.actif}"><span class="badge badge-point ton-succes">Actif</span></c:when>
                        <c:otherwise><span class="badge badge-point ton-neutre">Désactivé</span></c:otherwise>
                      </c:choose>
                    </td>
                    <td class="nombre"><app:date valeur="${u.dateCreation.toLocalDate()}"/></td>
                    <td class="cellule-actions">
                      <%-- Pas de bouton pour l'administrateur : le service refuse de toute façon de le désactiver --%>
                      <c:if test="${u.role != 'ADMIN'}">
                        <c:choose>
                          <c:when test="${u.actif}">
                            <form method="post" action="${pageContext.request.contextPath}/admin/utilisateurs/desactiver"
                                  data-confirmer="Désactiver le compte de <c:out value='${u.prenom} ${u.nom}'/> ?"
                                  data-confirmer-texte="Il ne pourra plus se connecter. Son historique est conservé et le compte pourra être réactivé."
                                  data-confirmer-bouton="Désactiver">
                              <input type="hidden" name="id" value="${u.id}">
                              <input type="hidden" name="role" value="${role}">
                              <button type="submit" class="bouton bouton-petit bouton-danger-doux">
                                <svg class="icone" aria-hidden="true"><use href="#i-cadenas"/></svg>Désactiver
                              </button>
                            </form>
                          </c:when>
                          <c:otherwise>
                            <form method="post" action="${pageContext.request.contextPath}/admin/utilisateurs/reactiver">
                              <input type="hidden" name="id" value="${u.id}">
                              <input type="hidden" name="role" value="${role}">
                              <button type="submit" class="bouton bouton-petit">
                                <svg class="icone" aria-hidden="true"><use href="#i-restaurer"/></svg>Réactiver
                              </button>
                            </form>
                          </c:otherwise>
                        </c:choose>
                      </c:if>
                    </td>
                  </tr>
                </c:forEach>
                <tr class="ligne-message" data-filtre-vide hidden>
                  <td colspan="6">Aucun utilisateur ne correspond à la recherche.</td>
                </tr>
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
