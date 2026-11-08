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
    <p>Le paiement a été enregistré.</p>
  </c:if>
  <%-- Erreur enregistrée avant la redirection (MessageFlash), affichée une seule fois --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <p>Client : <c:out value="${contrat.client.utilisateur.prenom} ${contrat.client.utilisateur.nom}"/>
    (<c:out value="${contrat.client.utilisateur.email}"/>)</p>
  <p>Type d'assurance : <c:out value="${contrat.typeAssurance.nom}"/></p>
  <p>Du <c:out value="${contrat.dateDebut}"/> au <c:out value="${contrat.dateFin}"/></p>
  <p>Prime annuelle : <c:out value="${contrat.prime}"/> €</p>
  <p>Statut : <c:out value="${contrat.statut}"/></p>

  <h2>Garanties</h2>
  <table border="1">
    <thead>
      <tr>
        <th>Garantie</th>
        <th>Plafond</th>
        <th>Franchise</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="g" items="${contrat.garanties}">
        <tr>
          <td><c:out value="${g.nom}"/></td>
          <td><c:out value="${g.plafond}"/> €</td>
          <td><c:out value="${g.franchise}"/> €</td>
        </tr>
      </c:forEach>
    </tbody>
  </table>

  <h2>Paiement</h2>
  <c:choose>
    <c:when test="${empty paiement}">
      <p>Aucun paiement n'est associé à ce contrat.</p>
    </c:when>
    <c:otherwise>
      <p>Montant : <c:out value="${paiement.montant}"/> €, à régler au plus tard le <c:out value="${paiement.dateEcheance}"/></p>
      <p>Statut : <c:out value="${paiement.statut}"/></p>
      <c:if test="${paiement.statut == 'EFFECTUE'}">
        <p>Payé le <c:out value="${paiement.datePaiement}"/> par <c:out value="${paiement.modePaiement}"/></p>
      </c:if>

      <%-- Formulaire pour l'agent seulement (l'admin consulte), tant que le contrat peut encore être payé :
           PaiementService refuse de toute façon un contrat déjà payé, résilié ou expiré --%>
      <c:if test="${sessionScope.utilisateur.role == 'AGENT'
                    and (paiement.statut == 'EN_ATTENTE' or paiement.statut == 'EN_RETARD')
                    and contrat.statut != 'RESILIE' and contrat.statut != 'EXPIRE'}">
        <h3>Enregistrer le paiement</h3>
        <form method="post" action="${pageContext.request.contextPath}/agent/contrats/paiement">
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
          <p>
            <label for="datePaiement">Date du paiement</label>
            <input type="date" id="datePaiement" name="datePaiement" value="${aujourdhui}" max="${aujourdhui}">
          </p>
          <button type="submit">Enregistrer le paiement de <c:out value="${paiement.montant}"/> €</button>
        </form>
      </c:if>
    </c:otherwise>
  </c:choose>

  <p><a href="${pageContext.request.contextPath}/agent/contrats">Retour à la liste des contrats</a></p>
</body>
</html>
