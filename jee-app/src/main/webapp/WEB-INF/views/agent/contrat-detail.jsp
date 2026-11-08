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

  <c:if test="${not empty param.resilie}">
    <p>Le contrat a été résilié.</p>
  </c:if>
  <%-- Erreur enregistrée avant la redirection (MessageFlash), affichée une seule fois --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <%-- Le paiement est affiché sans formulaire : c'est le client qui paie, depuis sa propre fiche --%>
  <%@ include file="/WEB-INF/views/common/contrat-infos.jspf" %>
  <%@ include file="/WEB-INF/views/common/contrat-resiliation.jspf" %>

  <p><a href="${pageContext.request.contextPath}/agent/contrats">Retour à la liste des contrats</a></p>
</body>
</html>
