<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"
        description="Libellé d'un type de sinistre : Accident, Dégâts des eaux..." %>
<%@ attribute name="valeur" required="true" type="java.lang.Object" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Utilisation : <app:typeSinistre valeur="${sinistre.typeSinistre}"/> --%>
<c:choose>
  <c:when test="${valeur == 'ACCIDENT'}">Accident</c:when>
  <c:when test="${valeur == 'VOL'}">Vol</c:when>
  <c:when test="${valeur == 'INCENDIE'}">Incendie</c:when>
  <c:when test="${valeur == 'DEGATS_DES_EAUX'}">Dégâts des eaux</c:when>
  <c:when test="${valeur == 'MALADIE'}">Maladie</c:when>
  <c:when test="${valeur == 'AUTRE'}">Autre</c:when>
  <c:otherwise><c:out value="${valeur}"/></c:otherwise>
</c:choose>
