<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Déclarer un sinistre</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Déclarer un sinistre</h1>

  <p>En cas d'urgence (personne blessée, danger immédiat), appelez d'abord les secours : 15, 17, 18 ou 112.</p>

  <c:if test="${not empty erreurs.global}">
    <p><c:out value="${erreurs.global}"/></p>
  </c:if>

  <c:choose>
    <c:when test="${empty contrats}">
      <p>Vous n'avez aucun contrat payé et commencé : vous ne pouvez pas encore déclarer de sinistre.</p>
    </c:when>
    <c:otherwise>
      <form method="post" action="${pageContext.request.contextPath}/client/sinistres/nouveau">
        <p>
          <label for="contrat">Contrat concerné</label>
          <select id="contrat" name="contrat" required>
            <option value="">-- Choisir --</option>
            <c:forEach var="ct" items="${contrats}">
              <option value="${ct.id}" ${ct.id == contrat ? 'selected' : ''}>
                <c:out value="n° ${ct.id} : ${ct.typeAssurance.nom}, du ${ct.dateDebut} au ${ct.dateFin} (${ct.statut})"/>
              </option>
            </c:forEach>
          </select>
          <c:out value="${erreurs.contrat}"/>
        </p>

        <p>
          <label for="dateSinistre">Date du sinistre</label>
          <%-- max : un sinistre ne peut pas être déclaré à l'avance (vérifié aussi par le service) --%>
          <input type="date" id="dateSinistre" name="dateSinistre" max="${aujourdhui}" required
                 value="<c:out value='${dateSinistre}'/>">
          <c:out value="${erreurs.dateSinistre}"/>
        </p>

        <p>
          <label for="type">Type de sinistre</label>
          <select id="type" name="type" required>
            <option value="">-- Choisir --</option>
            <c:forEach var="t" items="${types}">
              <option value="${t}" ${t == type ? 'selected' : ''}><c:out value="${t}"/></option>
            </c:forEach>
          </select>
          <c:out value="${erreurs.type}"/>
        </p>

        <p>
          <label for="description">Description des circonstances</label><br>
          <textarea id="description" name="description" rows="6" cols="70" maxlength="${longueurMax}"
                    required><c:out value="${description}"/></textarea>
          <c:out value="${erreurs.description}"/>
        </p>

        <p>
          <label for="montantEstime">Montant estimé des dommages, en € (facultatif)</label>
          <input type="text" id="montantEstime" name="montantEstime" inputmode="decimal"
                 value="<c:out value='${montantEstime}'/>">
          <c:out value="${erreurs.montantEstime}"/>
        </p>

        <button type="submit">Déclarer le sinistre</button>
      </form>

      <h2>Documents à préparer</h2>
      <p>Un agent pourra vous les demander pendant l'analyse de votre dossier.</p>
      <ul>
        <li>Pour tous les sinistres : photos des dommages, factures ou devis de réparation.</li>
        <li>Accident : constat amiable, permis de conduire, carte grise, coordonnées des témoins.</li>
        <li>Vol : récépissé du dépôt de plainte (obligatoire), factures ou photos des objets volés.</li>
        <li>Incendie : rapport des pompiers s'il y en a un, factures des biens détruits.</li>
        <li>Dégâts des eaux : constat amiable de dégât des eaux, facture du plombier.</li>
        <li>Maladie : ordonnances, feuilles de soins, décompte de la Sécurité sociale.</li>
      </ul>
    </c:otherwise>
  </c:choose>

  <p><a href="${pageContext.request.contextPath}/client/sinistres">Retour à mes sinistres</a></p>
</body>
</html>
