<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Garanties</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Garanties</h1>

  <c:if test="${not empty param.cree}">
    <p>La garantie a été créée.</p>
  </c:if>
  <c:if test="${not empty param.modifie}">
    <p>La garantie a été modifiée.</p>
  </c:if>
  <c:if test="${not empty param.supprime}">
    <p>La garantie a été supprimée.</p>
  </c:if>
  <%-- Erreur enregistrée avant la redirection (MessageFlash), affichée une seule fois --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <p><a href="${pageContext.request.contextPath}/agent/garanties/nouvelle">Nouvelle garantie</a></p>

  <table border="1">
    <thead>
      <tr>
        <th>Nom</th>
        <th>Description</th>
        <th>Plafond</th>
        <th>Franchise</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="garantie" items="${garanties}">
        <tr>
          <td><c:out value="${garantie.nom}"/></td>
          <td><c:out value="${garantie.description}"/></td>
          <%-- Plafond et franchise sont facultatifs : la base les accepte vides --%>
          <td>
            <c:choose>
              <c:when test="${garantie.plafond != null}"><c:out value="${garantie.plafond}"/> €</c:when>
              <c:otherwise>Aucun</c:otherwise>
            </c:choose>
          </td>
          <td>
            <c:choose>
              <c:when test="${garantie.franchise != null}"><c:out value="${garantie.franchise}"/> €</c:when>
              <c:otherwise>Aucune</c:otherwise>
            </c:choose>
          </td>
          <td>
            <a href="${pageContext.request.contextPath}/agent/garanties/modifier?id=${garantie.id}">Modifier</a>
            <%-- Suppression en POST uniquement ; le service refuse si la garantie est souscrite par un contrat --%>
            <form method="post" action="${pageContext.request.contextPath}/agent/garanties/supprimer"
                  onsubmit="return confirm('Supprimer cette garantie ?');">
              <input type="hidden" name="id" value="${garantie.id}">
              <button type="submit">Supprimer</button>
            </form>
          </td>
        </tr>
      </c:forEach>
      <c:if test="${empty garanties}">
        <tr>
          <td colspan="5">Aucune garantie pour l'instant.</td>
        </tr>
      </c:if>
    </tbody>
  </table>

  <p><a href="${pageContext.request.contextPath}${sessionScope.utilisateur.role.accueil}">Retour à mon espace</a></p>
</body>
</html>
