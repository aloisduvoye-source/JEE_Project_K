<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Contrat n° <c:out value="${contrat.id}"/></title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Contrat n° <c:out value="${contrat.id}"/></h1>

  <c:if test="${not empty param.paye}">
    <p>Votre paiement a été enregistré, merci.</p>
  </c:if>
  <c:if test="${not empty param.resilie}">
    <p>Le contrat a été résilié.</p>
  </c:if>
  <%-- Erreur enregistrée avant la redirection (MessageFlash), affichée une seule fois --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <%@ include file="/WEB-INF/views/common/contrat-infos.jspf" %>

  <%-- Formulaire tant que le contrat peut encore être payé : PaiementService refuse de toute façon
       un contrat déjà payé, résilié ou expiré --%>
  <c:if test="${not empty paiement
                and (paiement.statut == 'EN_ATTENTE' or paiement.statut == 'EN_RETARD')
                and contrat.statut != 'RESILIE' and contrat.statut != 'EXPIRE'}">
    <h3>Payer mon contrat</h3>
    <p>La prime se paie en une fois. Votre contrat sera actif à sa date de début, ou dès maintenant si elle est passée.</p>
    <form method="post" action="${pageContext.request.contextPath}/client/contrats/paiement">
      <input type="hidden" name="id" value="${contrat.id}">
      <p>
        <label for="mode">Mode de paiement</label>
        <select id="mode" name="mode">
          <option value="">Choisir…</option>
          <c:forEach var="m" items="${modes}">
            <option value="${m}">${m}</option>
          </c:forEach>
        </select>
      </p>
      <button type="submit">Payer <c:out value="${paiement.montant}"/> €</button>
    </form>
  </c:if>

  <%@ include file="/WEB-INF/views/common/contrat-resiliation.jspf" %>

  <p><a href="${pageContext.request.contextPath}/client/contrats">Retour à mes contrats</a></p>
</body>
</html>
