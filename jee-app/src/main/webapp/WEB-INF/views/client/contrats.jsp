<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Mes contrats</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Mes contrats</h1>

  <c:if test="${empty contrats}">
    <p>Vous n'avez aucun contrat pour l'instant.</p>
  </c:if>

  <c:forEach var="ct" items="${contrats}">
    <section>
      <h2>Contrat n° <c:out value="${ct.id}"/> : <c:out value="${ct.typeAssurance.nom}"/></h2>
      <p>
        Du <c:out value="${ct.dateDebut}"/> au <c:out value="${ct.dateFin}"/>,
        prime annuelle de <c:out value="${ct.prime}"/> €.
        Statut : <c:out value="${ct.statut}"/>
      </p>

      <table border="1">
        <thead>
          <tr>
            <th>Garantie</th>
            <th>Description</th>
            <th>Plafond</th>
            <th>Franchise</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="g" items="${ct.garanties}">
            <tr>
              <td><c:out value="${g.nom}"/></td>
              <td><c:out value="${g.description}"/></td>
              <td><c:out value="${g.plafond}"/> €</td>
              <td><c:out value="${g.franchise}"/> €</td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </section>
  </c:forEach>

  <p><a href="${pageContext.request.contextPath}/client/accueil">Retour à mon espace</a></p>
</body>
</html>
