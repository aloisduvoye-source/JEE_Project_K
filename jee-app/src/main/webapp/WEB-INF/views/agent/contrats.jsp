<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Contrats</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Contrats</h1>

  <c:if test="${not empty param.cree}">
    <p>Le contrat n° <c:out value="${param.cree}"/> a été créé. Il deviendra actif à sa date de début s'il est payé.</p>
  </c:if>
  <%-- Erreur enregistrée avant la redirection (MessageFlash), par exemple un contrat introuvable --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <%-- Seul un agent crée des contrats : l'admin consulte cette page sans le lien --%>
  <c:if test="${sessionScope.utilisateur.role == 'AGENT'}">
    <p><a href="${pageContext.request.contextPath}/agent/contrats/nouveau">Nouveau contrat</a></p>
  </c:if>

  <table border="1">
    <thead>
      <tr>
        <th>Numéro</th>
        <th>Client</th>
        <th>Type d'assurance</th>
        <th>Début</th>
        <th>Fin</th>
        <th>Prime annuelle</th>
        <th>Statut</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="ct" items="${contrats}">
        <tr>
          <td><a href="${pageContext.request.contextPath}/agent/contrats/detail?id=${ct.id}"><c:out value="${ct.id}"/></a></td>
          <td><c:out value="${ct.client.utilisateur.nom} ${ct.client.utilisateur.prenom}"/></td>
          <td><c:out value="${ct.typeAssurance.nom}"/></td>
          <td><c:out value="${ct.dateDebut}"/></td>
          <td><c:out value="${ct.dateFin}"/></td>
          <td><c:out value="${ct.prime}"/> €</td>
          <td><c:out value="${ct.statut}"/></td>
        </tr>
      </c:forEach>
      <c:if test="${empty contrats}">
        <tr>
          <td colspan="7">Aucun contrat pour l'instant.</td>
        </tr>
      </c:if>
    </tbody>
  </table>

  <%-- Page partagée agent / admin : retour vers l'espace du rôle connecté --%>
  <p><a href="${pageContext.request.contextPath}${sessionScope.utilisateur.role.accueil}">Retour à mon espace</a></p>
</body>
</html>
