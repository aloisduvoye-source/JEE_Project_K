<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"
        description="Badge coloré du statut d'un contrat" %>
<%@ attribute name="valeur" required="true" type="java.lang.Object" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Utilisation : <app:statutContrat valeur="${contrat.statut}"/> --%>
<c:choose>
  <c:when test="${valeur == 'ACTIF'}"><span class="badge badge-point ton-succes">Actif</span></c:when>
  <c:when test="${valeur == 'EN_ATTENTE'}"><span class="badge badge-point ton-attention">En attente</span></c:when>
  <c:when test="${valeur == 'SUSPENDU'}"><span class="badge badge-point ton-orange">Suspendu</span></c:when>
  <c:when test="${valeur == 'EXPIRE'}"><span class="badge badge-point ton-neutre">Expiré</span></c:when>
  <c:when test="${valeur == 'RESILIE'}"><span class="badge badge-point ton-danger">Résilié</span></c:when>
  <c:otherwise><span class="badge badge-point ton-neutre"><c:out value="${valeur}"/></span></c:otherwise>
</c:choose>
