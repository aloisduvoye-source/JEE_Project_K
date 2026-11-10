<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Sinistre n° <c:out value="${sinistre.id}"/></title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Sinistre n° <c:out value="${sinistre.id}"/></h1>

  <c:if test="${not empty param.declare}">
    <p>Votre sinistre a été déclaré. Un agent va l'analyser : vous pouvez suivre son avancement sur cette page.</p>
  </c:if>
  <%-- Erreur enregistrée avant la redirection (MessageFlash), affichée une seule fois --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <%@ include file="/WEB-INF/views/common/sinistre-infos.jspf" %>

  <p><a href="${pageContext.request.contextPath}/client/sinistres">Retour à mes sinistres</a></p>
</body>
</html>
