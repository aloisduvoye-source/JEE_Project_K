<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Types d'assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Types d'assurance</h1>

  <c:if test="${not empty param.cree}">
    <p>Le type d'assurance a été créé.</p>
  </c:if>
  <c:if test="${not empty param.modifie}">
    <p>Le type d'assurance a été modifié.</p>
  </c:if>
  <c:if test="${not empty param.supprime}">
    <p>Le type d'assurance a été supprimé.</p>
  </c:if>
  <c:if test="${not empty param.archive}">
    <p>Le type d'assurance a été archivé : il n'est plus proposé pour les nouveaux contrats.</p>
  </c:if>
  <c:if test="${not empty param.reactive}">
    <p>Le type d'assurance a été réactivé.</p>
  </c:if>
  <%-- Erreur enregistrée avant la redirection (MessageFlash), affichée une seule fois --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <p><a href="${pageContext.request.contextPath}/admin/types-assurance/nouveau">Nouveau type d'assurance</a></p>

  <table border="1">
    <thead>
      <tr>
        <th>Nom</th>
        <th>Description</th>
        <th>Prix de base</th>
        <th>Garanties disponibles</th>
        <th>État</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="type" items="${types}">
        <tr>
          <td><c:out value="${type.nom}"/></td>
          <td><c:out value="${type.description}"/></td>
          <td><c:out value="${type.prixBase}"/> €</td>
          <td>
            <%-- Noms des garanties séparés par des virgules --%>
            <c:forEach var="g" items="${type.garanties}" varStatus="etat">
              <c:out value="${g.nom}"/><c:if test="${not etat.last}">, </c:if>
            </c:forEach>
            <c:if test="${empty type.garanties}">Aucune</c:if>
          </td>
          <td>${type.actif ? 'Actif' : 'Archivé'}</td>
          <td>
            <a href="${pageContext.request.contextPath}/admin/types-assurance/modifier?id=${type.id}">Modifier</a>
            <%-- Archiver : alternative à la suppression pour un type utilisé par des contrats --%>
            <form method="post" action="${pageContext.request.contextPath}/admin/types-assurance/archiver">
              <input type="hidden" name="id" value="${type.id}">
              <input type="hidden" name="action" value="${type.actif ? 'archiver' : 'reactiver'}">
              <button type="submit">${type.actif ? 'Archiver' : 'Réactiver'}</button>
            </form>
            <%-- Suppression proposée seulement pour un type jamais utilisé (créé par erreur, doublon...) :
                 un type utilisé par un contrat s'archive. Le service refuse de toute façon la suppression. --%>
            <c:if test="${not typesUtilises.contains(type.id)}">
              <form method="post" action="${pageContext.request.contextPath}/admin/types-assurance/supprimer"
                    onsubmit="return confirm('Supprimer ce type d\'assurance ?');">
                <input type="hidden" name="id" value="${type.id}">
                <button type="submit">Supprimer</button>
              </form>
            </c:if>
          </td>
        </tr>
      </c:forEach>
      <c:if test="${empty types}">
        <tr>
          <td colspan="6">Aucun type d'assurance pour l'instant.</td>
        </tr>
      </c:if>
    </tbody>
  </table>

  <p><a href="${pageContext.request.contextPath}/admin/accueil">Retour à l'espace administrateur</a></p>
</body>
</html>
