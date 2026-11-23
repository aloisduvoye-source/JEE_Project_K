<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Mes garanties · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <%-- Nombre total de garanties, tous contrats confondus --%>
  <c:set var="nbGaranties" value="0"/>
  <c:forEach var="ct" items="${contrats}">
    <c:set var="nbGaranties" value="${nbGaranties + fn:length(ct.garanties)}"/>
  </c:forEach>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div class="entete-page">
        <div>
          <h1>Mes garanties</h1>
          <p class="sous-titre">
            <c:choose>
              <c:when test="${nbGaranties == 0}">Aucune garantie pour l'instant.</c:when>
              <c:otherwise>${nbGaranties} garantie${nbGaranties > 1 ? 's' : ''} sur ${fn:length(contrats)} contrat${fn:length(contrats) > 1 ? 's' : ''}.</c:otherwise>
            </c:choose>
          </p>
        </div>
      </div>

      <c:choose>
        <c:when test="${nbGaranties == 0}">
          <div class="carte vide">
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-bouclier-ok"/></svg></span>
            <h2>Aucune garantie</h2>
            <p>Les garanties de vos contrats apparaîtront ici dès qu'un conseiller les aura créés pour vous.</p>
          </div>
        </c:when>
        <c:otherwise>
          <div class="tableau-conteneur">
            <table class="tableau">
              <thead>
                <tr>
                  <th>Contrat</th>
                  <th>Statut</th>
                  <th>Garantie</th>
                  <th class="a-droite">Plafond</th>
                  <th class="a-droite">Franchise</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach var="ct" items="${contrats}">
                  <c:forEach var="g" items="${ct.garanties}">
                    <tr>
                      <td>
                        <a class="cellule-principale" href="${pageContext.request.contextPath}/client/contrats/detail?id=${ct.id}"><c:out value="${ct.typeAssurance.nom}"/></a>
                        <div class="cellule-secondaire">Contrat n° <c:out value="${ct.id}"/></div>
                      </td>
                      <td><app:statutContrat valeur="${ct.statut}"/></td>
                      <td>
                        <div class="cellule-principale"><c:out value="${g.nom}"/></div>
                        <c:if test="${not empty g.description}"><div class="cellule-secondaire"><c:out value="${g.description}"/></div></c:if>
                      </td>
                      <%-- Plafond et franchise sont facultatifs --%>
                      <td class="a-droite nombre"><app:euros valeur="${g.plafond}" vide="Aucun"/></td>
                      <td class="a-droite nombre"><app:euros valeur="${g.franchise}" vide="Aucune"/></td>
                    </tr>
                  </c:forEach>
                </c:forEach>
              </tbody>
            </table>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
