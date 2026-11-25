<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"
        description="Badge coloré du statut d'un sinistre" %>
<%@ attribute name="valeur" required="true" type="java.lang.Object" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Utilisation : <app:statutSinistre valeur="${sinistre.statut}"/> --%>
<c:choose>
  <c:when test="${valeur == 'DECLARE'}"><span class="badge badge-point ton-info">Déclaré</span></c:when>
  <c:when test="${valeur == 'EN_ANALYSE'}"><span class="badge badge-point ton-attention">En analyse</span></c:when>
  <c:when test="${valeur == 'ACCEPTE'}"><span class="badge badge-point ton-succes">Accepté</span></c:when>
  <c:when test="${valeur == 'REFUSE'}"><span class="badge badge-point ton-danger">Refusé</span></c:when>
  <c:when test="${valeur == 'INDEMNISE'}"><span class="badge badge-point ton-violet">Indemnisé</span></c:when>
  <c:otherwise><span class="badge badge-point ton-neutre"><c:out value="${valeur}"/></span></c:otherwise>
</c:choose>
