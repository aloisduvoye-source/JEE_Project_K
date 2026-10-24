<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Nouveau contrat</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Nouveau contrat</h1>

  <c:if test="${not empty erreurs.global}">
    <p><c:out value="${erreurs.global}"/></p>
  </c:if>

  <%-- Étape 1 : le type d'assurance détermine les garanties proposées --%>
  <form method="get" action="${pageContext.request.contextPath}/agent/contrats/nouveau">
    <p>
      <label for="choixType">Type d'assurance</label>
      <select id="choixType" name="type">
        <option value="">-- Choisir --</option>
        <c:forEach var="t" items="${types}">
          <option value="${t.id}" ${t.id == typeChoisi.id ? 'selected' : ''}>
            <c:out value="${t.nom}"/> (à partir de <c:out value="${t.prixBase}"/> €)
          </option>
        </c:forEach>
      </select>
      <button type="submit">Choisir ce type</button>
    </p>
  </form>

  <%-- Étape 2 : affichée une fois le type choisi --%>
  <c:if test="${not empty typeChoisi}">
    <form method="post" action="${pageContext.request.contextPath}/agent/contrats/nouveau">
      <input type="hidden" name="type" value="${typeChoisi.id}">

      <h2><c:out value="${typeChoisi.nom}"/></h2>

      <p>
        <label for="client">Client</label>
        <select id="client" name="client">
          <option value="">-- Choisir --</option>
          <c:forEach var="cl" items="${clients}">
            <option value="${cl.id}" ${cl.id == client ? 'selected' : ''}>
              <c:out value="${cl.utilisateur.nom} ${cl.utilisateur.prenom} (${cl.utilisateur.email})"/>
            </option>
          </c:forEach>
        </select>
        <c:out value="${erreurs.client}"/>
      </p>

      <fieldset>
        <legend>Garanties</legend>
        <c:forEach var="g" items="${garantiesDisponibles}">
          <p>
            <input type="checkbox" id="garantie${g.id}" name="garanties" value="${g.id}"
                   ${garantiesCochees.contains(g.id) ? 'checked' : ''}>
            <label for="garantie${g.id}">
              <c:out value="${g.nom}"/> (plafond <c:out value="${g.plafond}"/> €, franchise <c:out value="${g.franchise}"/> €)
            </label>
          </p>
        </c:forEach>
        <c:out value="${erreurs.garanties}"/>
      </fieldset>

      <p>
        <label for="dateDebut">Date de début</label>
        <input type="date" id="dateDebut" name="dateDebut" value="<c:out value='${dateDebut}'/>">
        <c:out value="${erreurs.dateDebut}"/>
      </p>
      <p>
        <label for="dateFin">Date de fin</label>
        <input type="date" id="dateFin" name="dateFin" value="<c:out value='${dateFin}'/>">
        <c:out value="${erreurs.dateFin}"/>
      </p>
      <p>
        <label for="prime">Prime annuelle (€)</label>
        <input type="text" id="prime" name="prime" value="<c:out value='${prime}'/>">
        (minimum <c:out value="${typeChoisi.prixBase}"/> €)
        <c:out value="${erreurs.prime}"/>
      </p>

      <button type="submit">Créer le contrat</button>
    </form>
  </c:if>

  <p><a href="${pageContext.request.contextPath}/agent/contrats">Retour à la liste des contrats</a></p>
</body>
</html>
