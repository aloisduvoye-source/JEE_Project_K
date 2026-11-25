<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"
        description="Badge coloré du statut d'un paiement" %>
<%@ attribute name="valeur" required="true" type="java.lang.Object" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Utilisation : <app:statutPaiement valeur="${paiement.statut}"/> --%>
<c:choose>
  <c:when test="${valeur == 'EFFECTUE'}"><span class="badge badge-point ton-succes">Payé</span></c:when>
  <c:when test="${valeur == 'EN_ATTENTE'}"><span class="badge badge-point ton-attention">À payer</span></c:when>
  <c:when test="${valeur == 'EN_RETARD'}"><span class="badge badge-point ton-danger">En retard</span></c:when>
  <c:when test="${valeur == 'ANNULE'}"><span class="badge badge-point ton-neutre">Annulé</span></c:when>
  <c:otherwise><span class="badge badge-point ton-neutre"><c:out value="${valeur}"/></span></c:otherwise>
</c:choose>
