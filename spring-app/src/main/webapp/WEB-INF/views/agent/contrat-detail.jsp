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

  <c:if test="${not empty param.modifie}">
    <p>Le contrat a été modifié.</p>
  </c:if>
  <c:if test="${not empty param.resilie}">
    <p>Le contrat a été résilié.</p>
  </c:if>
  <c:if test="${not empty param.renouvele}">
    <p>Ce contrat renouvelle le précédent. Il deviendra actif à sa date de début une fois sa prime payée.</p>
  </c:if>
  <c:if test="${not empty param.suspendu}">
    <p>Le contrat a été suspendu.</p>
  </c:if>
  <c:if test="${not empty param.reactive}">
    <p>La suspension a été levée.</p>
  </c:if>
  <%-- Erreur enregistrée avant la redirection (MessageFlash), affichée une seule fois --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <%-- Le paiement est affiché sans formulaire : c'est le client qui paie, depuis sa propre fiche --%>
  <%@ include file="/WEB-INF/views/common/contrat-infos.jspf" %>

  <%-- Modification par l'agent seulement, tant que le contrat n'est pas payé (ModifierContratServlet le vérifie aussi) --%>
  <c:if test="${sessionScope.utilisateur.role == 'AGENT'
                and (contrat.statut == 'EN_ATTENTE' or contrat.statut == 'SUSPENDU')
                and (empty paiement or paiement.statut != 'EFFECTUE')}">
    <p><a href="${pageContext.request.contextPath}/agent/contrats/modifier?id=${contrat.id}">Modifier le contrat</a></p>
  </c:if>

  <%-- Suspension par un agent seulement (ActionContratServlet et ContratService le vérifient aussi) --%>
  <c:if test="${sessionScope.utilisateur.role == 'AGENT'}">
    <c:if test="${contrat.statut == 'ACTIF'}">
      <h2>Suspendre le contrat</h2>
      <p>Pendant la suspension, le contrat ne couvre plus les sinistres. Elle dure jusqu'à ce qu'un agent la lève.</p>
      <form method="post" action="${pageContext.request.contextPath}/agent/contrats/suspendre"
            onsubmit="return confirm('Suspendre ce contrat ?');">
        <input type="hidden" name="id" value="${contrat.id}">
        <p>
          <label for="motifSuspension">Motif (visible par le client)</label>
          <input type="text" id="motifSuspension" name="motif" maxlength="255" required>
        </p>
        <button type="submit">Suspendre le contrat</button>
      </form>
    </c:if>
    <c:if test="${contrat.suspenduParAgent}">
      <h2>Lever la suspension</h2>
      <form method="post" action="${pageContext.request.contextPath}/agent/contrats/lever-suspension">
        <input type="hidden" name="id" value="${contrat.id}">
        <button type="submit">Lever la suspension</button>
      </form>
    </c:if>
  </c:if>

  <%@ include file="/WEB-INF/views/common/contrat-renouvellement.jspf" %>

  <%@ include file="/WEB-INF/views/common/contrat-resiliation.jspf" %>

  <p><a href="${pageContext.request.contextPath}/agent/contrats">Retour à la liste des contrats</a></p>
</body>
</html>
