<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Mes paiements</title>
</head>
<body>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<h1>Mes paiements</h1>

<form method="get" action="${pageContext.request.contextPath}/client/paiements">
    <label for="statut">Statut :</label>
    <select id="statut" name="statut">
        <option value="">Tous</option>
        <c:forEach var="s" items="${statuts}">
            <option value="${s}" ${s == statutSelectionne ? 'selected' : ''}><c:out value="${s}"/></option>
        </c:forEach>
    </select>

    <label for="mode">Mode :</label>
    <select id="mode" name="mode">
        <option value="">Tous</option>
        <c:forEach var="m" items="${modes}">
            <option value="${m}" ${m == modeSelectionne ? 'selected' : ''}><c:out value="${m}"/></option>
        </c:forEach>
    </select>

    <label for="tri">Trier par :</label>
    <select id="tri" name="tri">
        <option value="echeance_desc" ${triSelectionne == 'echeance_desc' ? 'selected' : ''}>Échéance (plus récente)</option>
        <option value="echeance_asc" ${triSelectionne == 'echeance_asc' ? 'selected' : ''}>Échéance (plus ancienne)</option>
        <option value="montant_desc" ${triSelectionne == 'montant_desc' ? 'selected' : ''}>Montant (décroissant)</option>
        <option value="montant_asc" ${triSelectionne == 'montant_asc' ? 'selected' : ''}>Montant (croissant)</option>
        <option value="paiement_desc" ${triSelectionne == 'paiement_desc' ? 'selected' : ''}>Date de paiement (récente)</option>
    </select>

    <button type="submit">Filtrer</button>
    <a href="${pageContext.request.contextPath}/client/paiements">Réinitialiser</a>
</form>

<br>

<c:if test="${empty paiements}">
    <p>Aucun paiement trouvé.</p>
</c:if>

<c:if test="${not empty paiements}">
    <table border="1">
        <thead>
        <tr>
            <th>Contrat</th>
            <th>Échéance</th>
            <th>Montant</th>
            <th>Date de paiement</th>
            <th>Mode</th>
            <th>Statut</th>
            <th>Action</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="p" items="${paiements}">
            <tr>
                <td>Contrat n° <c:out value="${p.contrat.id}"/> (<c:out value="${p.contrat.typeAssurance.nom}"/>)</td>
                <td><c:out value="${p.dateEcheance}"/></td>
                <td><c:out value="${p.montant}"/> €</td>
                <td><c:out value="${empty p.datePaiement ? '-' : p.datePaiement}"/></td>
                <td><c:out value="${empty p.modePaiement ? '-' : p.modePaiement}"/></td>
                <td><c:out value="${p.statut}"/></td>
                <td>
                    <%-- Le formulaire de paiement est sur la fiche du contrat, mêmes conditions que là-bas --%>
                    <c:set var="aPayer" value="${(p.statut == 'EN_ATTENTE' or p.statut == 'EN_RETARD')
                                                 and p.contrat.statut != 'RESILIE' and p.contrat.statut != 'EXPIRE'}"/>
                    <a href="${pageContext.request.contextPath}/client/contrats/detail?id=${p.contrat.id}">${aPayer ? 'Payer' : 'Voir le contrat'}</a>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<p><a href="${pageContext.request.contextPath}/client/accueil">Retour à mon espace</a></p>
</body>
</html>
