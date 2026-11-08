<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"
        description="Libellé d'un mode de paiement : Carte bancaire, Virement..." %>
<%@ attribute name="valeur" required="true" type="java.lang.Object" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Utilisation : <app:modePaiement valeur="${paiement.modePaiement}"/> --%>
<c:choose>
  <c:when test="${valeur == 'CARTE'}">Carte bancaire</c:when>
  <c:when test="${valeur == 'VIREMENT'}">Virement</c:when>
  <c:when test="${valeur == 'PRELEVEMENT'}">Prélèvement</c:when>
  <c:when test="${valeur == 'CHEQUE'}">Chèque</c:when>
  <c:otherwise><c:out value="${valeur}"/></c:otherwise>
</c:choose>
