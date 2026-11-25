<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Mes garanties</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Mes garanties</h1>

  <table border="1">
    <thead>
      <tr>
        <th>Contrat</th>
        <th>Statut du contrat</th>
        <th>Garantie</th>
        <th>Description</th>
        <th>Plafond</th>
        <th>Franchise</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="ct" items="${contrats}">
        <c:forEach var="g" items="${ct.garanties}">
          <tr>
            <td>
              <a href="${pageContext.request.contextPath}/client/contrats/detail?id=${ct.id}">n° <c:out value="${ct.id}"/></a>
              (<c:out value="${ct.typeAssurance.nom}"/>)
            </td>
            <td><c:out value="${ct.statut}"/></td>
            <td><c:out value="${g.nom}"/></td>
            <td><c:out value="${g.description}"/></td>
            <%-- Plafond et franchise sont facultatifs --%>
            <td>
              <c:choose>
                <c:when test="${g.plafond != null}"><c:out value="${g.plafond}"/> €</c:when>
                <c:otherwise>Aucun</c:otherwise>
              </c:choose>
            </td>
            <td>
              <c:choose>
                <c:when test="${g.franchise != null}"><c:out value="${g.franchise}"/> €</c:when>
                <c:otherwise>Aucune</c:otherwise>
              </c:choose>
            </td>
          </tr>
        </c:forEach>
      </c:forEach>
      <c:if test="${empty contrats}">
        <tr>
          <td colspan="6">Aucune garantie pour l'instant.</td>
        </tr>
      </c:if>
    </tbody>
  </table>

  <p><a href="${pageContext.request.contextPath}/client/accueil">Retour à mon espace</a></p>
</body>
</html>
