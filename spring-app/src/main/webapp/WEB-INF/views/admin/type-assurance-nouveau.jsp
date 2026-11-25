<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Nouveau type d'assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Nouveau type d'assurance</h1>

  <c:if test="${not empty erreurs.global}">
    <p><c:out value="${erreurs.global}"/></p>
  </c:if>

  <form method="post" action="${pageContext.request.contextPath}/admin/types-assurance/nouveau">
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
      <label for="prixBase">Prix de base annuel (€)</label>
      <input type="text" id="prixBase" name="prixBase" value="<c:out value='${prixBase}'/>">
      (prime minimale d'un contrat de ce type, pour un an)
      <c:out value="${erreurs.prixBase}"/>
    </p>

    <fieldset>
      <legend>Garanties proposées</legend>
      <c:forEach var="g" items="${garanties}">
        <p>
          <input type="checkbox" id="garantie${g.id}" name="garanties" value="${g.id}"
                 ${garantiesCochees.contains(g.id) ? 'checked' : ''}>
          <label for="garantie${g.id}">
            <c:out value="${g.nom}"/> (plafond <c:out value="${g.plafond}"/> €, franchise <c:out value="${g.franchise}"/> €)
          </label>
        </p>
      </c:forEach>
      <c:if test="${empty garanties}">
        <p>Aucune garantie n'existe pour l'instant.</p>
      </c:if>
      <c:out value="${erreurs.garanties}"/>
    </fieldset>

    <button type="submit">Créer le type d'assurance</button>
  </form>

  <p><a href="${pageContext.request.contextPath}/admin/types-assurance">Retour à la liste des types d'assurance</a></p>
</body>
</html>
