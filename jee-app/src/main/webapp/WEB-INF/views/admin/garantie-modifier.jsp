<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Modifier une garantie</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Modifier « <c:out value="${garantie.nom}"/> »</h1>

  <c:if test="${not empty erreurs.global}">
    <p><c:out value="${erreurs.global}"/></p>
  </c:if>

  <form method="post" action="${pageContext.request.contextPath}/admin/garanties/modifier">
    <input type="hidden" name="id" value="${garantie.id}">
    <p>
      <label for="nom">Nom</label>
      <input type="text" id="nom" name="nom" maxlength="100" value="<c:out value='${nom}'/>">
      <c:out value="${erreurs.nom}"/>
    </p>
    <p>
      <label for="description">Description</label><br>
      <textarea id="description" name="description" rows="4" cols="60" maxlength="1000"><c:out value="${description}"/></textarea>
      <c:out value="${erreurs.description}"/>
    </p>
    <p>
      <label for="plafond">Plafond (€)</label>
      <input type="text" id="plafond" name="plafond" value="<c:out value='${plafond}'/>">
      (montant maximum remboursé, facultatif)
      <c:out value="${erreurs.plafond}"/>
    </p>
    <p>
      <label for="franchise">Franchise (€)</label>
      <input type="text" id="franchise" name="franchise" value="<c:out value='${franchise}'/>">
      (montant restant à la charge du client, facultatif)
      <c:out value="${erreurs.franchise}"/>
    </p>
    <button type="submit">Enregistrer</button>
  </form>

  <p><a href="${pageContext.request.contextPath}/admin/garanties">Retour à la liste des garanties</a></p>
</body>
</html>
