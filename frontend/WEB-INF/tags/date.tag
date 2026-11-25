<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"
        description="Date (LocalDate) au format français court : 5 nov. 2026" %>
<%@ attribute name="valeur" required="true" type="java.time.LocalDate" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  Utilisation : <app:date valeur="${contrat.dateDebut}"/>
  JSTL (fmt:formatDate) ne sait pas formater les LocalDate : le mois est pris dans cette liste.
  Espaces insécables (&#160;) entre les morceaux : une simple espace entre deux ${...} serait
  supprimée par trimDirectiveWhitespaces, et la date ne doit pas être coupée en fin de ligne.
--%>
<c:set var="mois" value="${fn:split('janv.,févr.,mars,avr.,mai,juin,juil.,août,sept.,oct.,nov.,déc.', ',')}"/>
<c:choose>
  <c:when test="${empty valeur}">—</c:when>
  <c:otherwise><time datetime="${valeur}">${valeur.dayOfMonth}&#160;${mois[valeur.monthValue - 1]}&#160;${valeur.year}</time></c:otherwise>
</c:choose>
