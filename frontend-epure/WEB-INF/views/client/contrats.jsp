<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Mes contrats</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Mes contrats</h1>

  <%-- Erreur enregistrée avant la redirection (MessageFlash), par exemple un contrat introuvable --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <form method="get" action="${pageContext.request.contextPath}/client/contrats">
    <label for="statut">Statut</label>
    <select id="statut" name="statut">
      <option value="">Tous</option>
      <c:forEach var="st" items="${statuts}">
        <option value="${st}" ${st == statutChoisi ? 'selected' : ''}><c:out value="${st}"/></option>
      </c:forEach>
    </select>

    <label for="type">Type</label>
    <select id="type" name="type">
      <option value="">Tous</option>
      <c:forEach var="t" items="${types}">
        <option value="${t.id}" ${t.id == typeChoisi ? 'selected' : ''}><c:out value="${t.nom}"/></option>
      </c:forEach>
    </select>

    <label for="tri">Trier par</label>
    <select id="tri" name="tri">
      <option value="debut_desc" ${triChoisi == 'debut_desc' ? 'selected' : ''}>Début (plus récent)</option>
      <option value="debut_asc" ${triChoisi == 'debut_asc' ? 'selected' : ''}>Début (plus ancien)</option>
      <option value="fin_asc" ${triChoisi == 'fin_asc' ? 'selected' : ''}>Fin (plus proche)</option>
      <option value="fin_desc" ${triChoisi == 'fin_desc' ? 'selected' : ''}>Fin (plus lointaine)</option>
      <option value="prime_desc" ${triChoisi == 'prime_desc' ? 'selected' : ''}>Prime (décroissante)</option>
      <option value="prime_asc" ${triChoisi == 'prime_asc' ? 'selected' : ''}>Prime (croissante)</option>
    </select>

    <button type="submit">Filtrer</button>
    <c:if test="${recherche}">
      <a href="${pageContext.request.contextPath}/client/contrats">Effacer</a>
    </c:if>
  </form>

  <c:if test="${empty contrats}">
    <p>${recherche ? 'Aucun contrat ne correspond à ces critères.' : "Vous n'avez aucun contrat pour l'instant."}</p>
  </c:if>

  <c:forEach var="ct" items="${contrats}">
    <section>
      <h2>Contrat n° <c:out value="${ct.id}"/> : <c:out value="${ct.typeAssurance.nom}"/></h2>
      <p>
        Du <c:out value="${ct.dateDebut}"/> au <c:out value="${ct.dateFin}"/>,
        prime annuelle de <c:out value="${ct.prime}"/> €.
        Statut : <c:out value="${ct.statut}"/>
      </p>
      <p><a href="${pageContext.request.contextPath}/client/contrats/detail?id=${ct.id}">Voir le contrat et son paiement</a></p>

      <table border="1">
        <thead>
          <tr>
            <th>Garantie</th>
            <th>Description</th>
            <th>Plafond</th>
            <th>Franchise</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="g" items="${ct.garanties}">
            <tr>
              <td><c:out value="${g.nom}"/></td>
              <td><c:out value="${g.description}"/></td>
              <td><c:out value="${g.plafond}"/> €</td>
              <td><c:out value="${g.franchise}"/> €</td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </section>
  </c:forEach>

  <p><a href="${pageContext.request.contextPath}/client/accueil">Retour à mon espace</a></p>
</body>
</html>
