<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Modifier un type d'assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Modifier « <c:out value="${type.nom}"/> »</h1>

  <c:if test="${not empty erreurs.global}">
    <p><c:out value="${erreurs.global}"/></p>
  </c:if>
  <c:if test="${not empty erreurs.garantie}">
    <p><c:out value="${erreurs.garantie}"/></p>
  </c:if>
  <c:if test="${not empty param.garanties}">
    <p>Les garanties proposées ont été mises à jour.</p>
  </c:if>

  <c:set var="action" value="${pageContext.request.contextPath}/admin/types-assurance/modifier"/>

  <h2>Informations</h2>
  <form method="post" action="${action}">
    <input type="hidden" name="id" value="${type.id}">
    <input type="hidden" name="action" value="infos">
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
      (ne s'applique qu'aux nouveaux contrats)
      <c:out value="${erreurs.prixBase}"/>
    </p>
    <button type="submit">Enregistrer</button>
  </form>

  <h2>Garanties proposées</h2>
  <p>Retirer une garantie ne modifie pas les contrats existants : elle n'est plus proposée pour les nouveaux contrats.</p>
  <table border="1">
    <thead>
      <tr>
        <th>Garantie</th>
        <th>Plafond</th>
        <th>Franchise</th>
        <th>Action</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="g" items="${garantiesProposees}">
        <tr>
          <td><c:out value="${g.nom}"/></td>
          <td><c:out value="${g.plafond}"/> €</td>
          <td><c:out value="${g.franchise}"/> €</td>
          <td>
            <%-- Un type doit garder au moins une garantie : pas de bouton sur la dernière --%>
            <c:choose>
              <c:when test="${fn:length(garantiesProposees) > 1}">
                <form method="post" action="${action}">
                  <input type="hidden" name="id" value="${type.id}">
                  <input type="hidden" name="action" value="retirer">
                  <input type="hidden" name="garantie" value="${g.id}">
                  <button type="submit">Retirer</button>
                </form>
              </c:when>
              <c:otherwise>Dernière garantie</c:otherwise>
            </c:choose>
          </td>
        </tr>
      </c:forEach>
    </tbody>
  </table>

  <h2>Ajouter une garantie</h2>
  <c:choose>
    <c:when test="${not empty garantiesAutres}">
      <form method="post" action="${action}">
        <input type="hidden" name="id" value="${type.id}">
        <input type="hidden" name="action" value="ajouter">
        <label for="garantie">Garantie</label>
        <select id="garantie" name="garantie">
          <c:forEach var="g" items="${garantiesAutres}">
            <option value="${g.id}"><c:out value="${g.nom}"/></option>
          </c:forEach>
        </select>
        <button type="submit">Ajouter</button>
      </form>
    </c:when>
    <c:otherwise>
      <p>Toutes les garanties existantes sont déjà proposées.</p>
    </c:otherwise>
  </c:choose>

  <p><a href="${pageContext.request.contextPath}/admin/types-assurance">Retour à la liste des types d'assurance</a></p>
</body>
</html>
