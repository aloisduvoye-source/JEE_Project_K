<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"
        description="Montant en euros au format français : 1 250,00 €" %>
<%@ attribute name="valeur" required="true" type="java.lang.Object" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%--
  Utilisation : <%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>  puis  <app:euros valeur="${contrat.prime}"/>
  La langue française (virgule décimale, espace entre les milliers) est fixée dans web.xml.
--%>
<fmt:formatNumber value="${valeur}" type="currency" currencyCode="EUR"/>
