<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"
        description="Montant en euros au format français : 1 250,00 €" %>
<%@ attribute name="valeur" required="true" type="java.lang.Object" %>
<%@ attribute name="vide" required="false" description="Texte affiché si le montant est absent (null)" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%--
  Utilisation : <%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>  puis  <app:euros valeur="${contrat.prime}"/>
  Montant facultatif (plafond, franchise d'une garantie) : <app:euros valeur="${g.plafond}" vide="Aucun"/>
  La langue française (virgule décimale, espace entre les milliers) est fixée dans web.xml.
--%>
<c:choose><c:when test="${empty valeur}"><c:out value="${vide}"/></c:when><c:otherwise><fmt:formatNumber value="${valeur}" type="currency" currencyCode="EUR"/></c:otherwise></c:choose>
