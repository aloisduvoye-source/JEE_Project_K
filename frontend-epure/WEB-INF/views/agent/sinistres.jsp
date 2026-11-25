<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Sinistres</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Sinistres</h1>

  <%-- Erreur enregistrée avant la redirection (MessageFlash), par exemple un sinistre introuvable --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <%-- Filtres en GET : l'adresse peut être gardée en favori (ex. ?statut=DECLARE pour la file de traitement) --%>
  <form method="get" action="${pageContext.request.contextPath}/agent/sinistres">
    <label for="statut">Statut</label>
    <select id="statut" name="statut">
      <option value="">Tous</option>
      <c:forEach var="st" items="${statuts}">
        <option value="${st}" ${st == statutChoisi ? 'selected' : ''}><c:out value="${st}"/></option>
      </c:forEach>
    </select>
    <%-- Seul un agent a des dossiers attribués : l'admin consulte sans ce filtre --%>
    <c:if test="${sessionScope.utilisateur.role == 'AGENT'}">
      <input type="checkbox" id="mien" name="mien" value="1" ${mien ? 'checked' : ''}>
      <label for="mien">Mes dossiers seulement</label>
    </c:if>
    <button type="submit">Filtrer</button>
  </form>
  <p><a href="${pageContext.request.contextPath}/agent/sinistres?statut=DECLARE">À prendre en charge</a></p>

  <table border="1">
    <thead>
      <tr>
        <th>Numéro</th>
        <th>Client</th>
        <th>Contrat</th>
        <th>Date du sinistre</th>
        <th>Déclaré le</th>
        <th>Type</th>
        <th>Montant estimé</th>
        <th>Agent</th>
        <th>Statut</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="s" items="${sinistres}">
        <tr>
          <td><a href="${pageContext.request.contextPath}/agent/sinistres/detail?id=${s.id}"><c:out value="${s.id}"/></a></td>
          <td><c:out value="${s.contrat.client.utilisateur.nom} ${s.contrat.client.utilisateur.prenom}"/></td>
          <td>n° <c:out value="${s.contrat.id}"/> (<c:out value="${s.contrat.typeAssurance.nom}"/>)</td>
          <td><c:out value="${s.dateSinistre}"/></td>
          <td><c:out value="${s.dateDeclaration.toLocalDate()}"/></td>
          <td><c:out value="${s.typeSinistre}"/></td>
          <td><c:if test="${not empty s.montantEstime}"><c:out value="${s.montantEstime}"/> €</c:if></td>
          <td><c:out value="${empty s.agent ? '-' : s.agent.nom}"/></td>
          <td><c:out value="${s.statut}"/></td>
        </tr>
      </c:forEach>
      <c:if test="${empty sinistres}">
        <tr>
          <td colspan="9">Aucun sinistre.</td>
        </tr>
      </c:if>
    </tbody>
  </table>

  <%-- Page partagée agent / admin : retour vers l'espace du rôle connecté --%>
  <p><a href="${pageContext.request.contextPath}${sessionScope.utilisateur.role.accueil}">Retour à mon espace</a></p>
</body>
</html>
