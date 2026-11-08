<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Modifier le contrat n° <c:out value="${contrat.id}"/></title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Modifier le contrat n° <c:out value="${contrat.id}"/></h1>

  <c:if test="${not empty erreurs.global}">
    <p><c:out value="${erreurs.global}"/></p>
  </c:if>

  <%-- Client et type affichés sans pouvoir être changés : sinon, c'est un autre contrat --%>
  <p>Client : <c:out value="${contrat.client.utilisateur.prenom} ${contrat.client.utilisateur.nom}"/></p>
  <p>Type d'assurance : <c:out value="${contrat.typeAssurance.nom}"/></p>
  <p>Le contrat n'est pas encore payé : le montant et l'échéance du paiement suivront ces modifications.</p>

  <form method="post" action="${pageContext.request.contextPath}/agent/contrats/modifier">
    <input type="hidden" name="id" value="${contrat.id}">

    <fieldset>
      <legend>Garanties</legend>
      <c:forEach var="g" items="${garantiesDisponibles}">
        <p>
          <input type="checkbox" id="garantie${g.id}" name="garanties" value="${g.id}"
                 ${garantiesCochees.contains(g.id) ? 'checked' : ''}>
          <label for="garantie${g.id}">
            <c:out value="${g.nom}"/> (plafond <c:out value="${g.plafond}"/> €, franchise <c:out value="${g.franchise}"/> €)
          </label>
        </p>
      </c:forEach>
      <c:out value="${erreurs.garanties}"/>
    </fieldset>

    <p>
      <label for="dateDebut">Date de début</label>
      <input type="date" id="dateDebut" name="dateDebut" value="<c:out value='${dateDebut}'/>" min="${aujourdhui}">
      <c:out value="${erreurs.dateDebut}"/>
    </p>
    <p>Durée : ${dureeEnMois} mois à partir de la date de début.</p>
    <p>
      <label for="prime">Prime annuelle (€)</label>
      <input type="text" id="prime" name="prime" value="<c:out value='${prime}'/>">
      <c:out value="${erreurs.prime}"/>
    </p>

    <button type="submit">Enregistrer les modifications</button>
  </form>

  <p><a href="${pageContext.request.contextPath}/agent/contrats/detail?id=${contrat.id}">Annuler</a></p>
</body>
</html>
