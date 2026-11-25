<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"
        description="Date et heure (LocalDateTime) au format français court : 5 nov. 2026 à 14:32" %>
<%@ attribute name="valeur" required="true" type="java.time.LocalDateTime" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<%--
  Utilisation : <app:dateHeure valeur="${message.dateMessage}"/>
  La date est écrite par <app:date>, l'heure sur deux chiffres (14:05 et non 14:5).
--%>
<c:choose>
  <c:when test="${empty valeur}">—</c:when>
  <c:otherwise><app:date valeur="${valeur.toLocalDate()}"/>&#160;à&#160;${valeur.hour lt 10 ? '0' : ''}${valeur.hour}:${valeur.minute lt 10 ? '0' : ''}${valeur.minute}</c:otherwise>
</c:choose>
