<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Sinistre n° <c:out value="${sinistre.id}"/></title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Sinistre n° <c:out value="${sinistre.id}"/></h1>

  <%-- Erreur enregistrée avant la redirection (MessageFlash), affichée une seule fois --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><c:out value="${sessionScope.messageErreur}"/></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <%@ include file="/WEB-INF/views/common/sinistre-infos.jspf" %>
  <p>Client : <c:out value="${sinistre.contrat.client.utilisateur.email}"/>
    (<a href="${pageContext.request.contextPath}/agent/contrats/detail?id=${sinistre.contrat.id}">voir le contrat</a>)</p>

  <h2>Garanties du contrat</h2>
  <%-- Ce que donnerait l'estimation du client avec chaque garantie : min(estimation - franchise, plafond) --%>
  <table border="1">
    <thead>
      <tr>
        <th>Garantie</th>
        <th>Plafond</th>
        <th>Franchise</th>
        <th>Avec l'estimation du client</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="p" items="${propositions}">
        <tr>
          <td><c:out value="${p.garantie.nom}"/></td>
          <td><c:out value="${empty p.garantie.plafond ? '-' : p.garantie.plafond}"/></td>
          <td><c:out value="${empty p.garantie.franchise ? '-' : p.garantie.franchise}"/></td>
          <td><c:out value="${empty p.montant ? 'pas de montant estimé' : p.montant.toPlainString().concat(' €')}"/></td>
        </tr>
      </c:forEach>
      <c:if test="${empty propositions}">
        <tr>
          <td colspan="4">Ce contrat n'a aucune garantie : le sinistre ne peut normalement pas être couvert.</td>
        </tr>
      </c:if>
    </tbody>
  </table>

  <%@ include file="/WEB-INF/views/common/sinistre-pieces.jspf" %>

  <%@ include file="/WEB-INF/views/common/sinistre-discussion.jspf" %>

  <%-- Actions selon le statut, pour un agent seulement : l'admin consulte.
       ActionSinistreServlet et SinistreService vérifient de toute façon le rôle et le statut. --%>
  <c:if test="${sessionScope.utilisateur.role == 'AGENT'}">
    <c:set var="actionUrl" value="${pageContext.request.contextPath}/agent/sinistres/action"/>
    <c:choose>
      <c:when test="${sinistre.statut == 'DECLARE'}">
        <h2>Traitement</h2>
        <form method="post" action="${actionUrl}">
          <input type="hidden" name="id" value="${sinistre.id}">
          <input type="hidden" name="action" value="prendre">
          <button type="submit">Prendre en charge ce dossier</button>
        </form>
      </c:when>

      <c:when test="${sinistre.statut == 'EN_ANALYSE'}">
        <h2>Décision</h2>
        <form method="post" action="${actionUrl}"
              onsubmit="return confirm('Accepter ce sinistre ? Il pourra ensuite être indemnisé.');">
          <input type="hidden" name="id" value="${sinistre.id}">
          <input type="hidden" name="action" value="accepter">
          <button type="submit">Accepter le sinistre</button>
        </form>
        <form method="post" action="${actionUrl}"
              onsubmit="return confirm('Refuser ce sinistre ? Cette décision est définitive.');">
          <input type="hidden" name="id" value="${sinistre.id}">
          <input type="hidden" name="action" value="refuser">
          <p>
            <label for="motif">Motif du refus (visible par le client)</label><br>
            <textarea id="motif" name="motif" rows="3" cols="70" maxlength="1000" required></textarea>
          </p>
          <button type="submit">Refuser le sinistre</button>
        </form>
      </c:when>

      <c:when test="${sinistre.statut == 'ACCEPTE'}">
        <h2>Indemnisation</h2>
        <p>
          Choisissez la garantie qui s'applique et le dommage retenu après examen des pièces.
          Le montant versé est calculé : dommage retenu moins la franchise, dans la limite du plafond.
          Le versement est daté du jour et le dossier est ensuite clos.
        </p>
        <%-- data-estime : montant estimé par le client, pour prévenir l'agent si le dommage retenu le dépasse --%>
        <form method="post" action="${actionUrl}" id="formIndemnisation" data-estime="${sinistre.montantEstime}">
          <input type="hidden" name="id" value="${sinistre.id}">
          <input type="hidden" name="action" value="indemniser">
          <p>
            <label for="garantie">Garantie appliquée</label>
            <select id="garantie" name="garantie" required>
              <option value="">-- Choisir --</option>
              <c:forEach var="p" items="${propositions}">
                <option value="${p.garantie.id}" data-franchise="${p.garantie.franchise}" data-plafond="${p.garantie.plafond}">
                  <c:out value="${p.garantie.nom}"/>
                </option>
              </c:forEach>
            </select>
          </p>
          <p>
            <label for="dommage">Dommage retenu, en €</label>
            <%-- Prérempli avec l'estimation du client : l'agent la corrige d'après les pièces --%>
            <input type="text" id="dommage" name="dommage" inputmode="decimal" required
                   value="<c:out value='${sinistre.montantEstime}'/>">
          </p>
          <p id="calcul">Montant versé : choisissez une garantie.</p>
          <button type="submit">Indemniser le client</button>
        </form>

        <script>
          // Aperçu du calcul fait par SinistreService.indemniser (qui reste seul juge) : min(dommage - franchise, plafond)
          (function () {
            var formulaire = document.getElementById('formIndemnisation');
            var garantie = document.getElementById('garantie');
            var dommage = document.getElementById('dommage');
            var calcul = document.getElementById('calcul');

            function montant(texte) {
              var valeur = parseFloat(String(texte).replace(',', '.'));
              return isNaN(valeur) ? null : valeur;
            }

            function calculer() {
              var option = garantie.options[garantie.selectedIndex];
              var d = montant(dommage.value);
              if (!option.value || d === null) {
                calcul.textContent = 'Montant versé : choisissez une garantie et indiquez le dommage retenu.';
                return null;
              }
              var franchise = montant(option.dataset.franchise) || 0;
              var plafond = montant(option.dataset.plafond);
              var resultat = d - franchise;
              var texte = d.toFixed(2) + ' € - franchise ' + franchise.toFixed(2) + ' €';
              if (plafond !== null && resultat > plafond) {
                resultat = plafond;
                texte += ', limité au plafond de ' + plafond.toFixed(2) + ' €';
              }
              resultat = Math.max(resultat, 0);
              calcul.textContent = 'Montant versé : ' + resultat.toFixed(2) + ' € (' + texte + ')'
                  + (resultat === 0 ? ' : rien à verser, le sinistre doit être refusé.' : '');
              return resultat;
            }

            garantie.addEventListener('change', calculer);
            dommage.addEventListener('input', calculer);

            formulaire.addEventListener('submit', function (e) {
              var resultat = calculer();
              var d = montant(dommage.value);
              var estime = montant(formulaire.dataset.estime);
              var message = 'Verser ' + (resultat === null ? '?' : resultat.toFixed(2))
                  + ' € au client ? Cette action est définitive.';
              // Point d'attention, pas un blocage : le client peut avoir sous-estimé ses dommages
              if (estime !== null && d !== null && d > estime) {
                message = 'Le dommage retenu (' + d.toFixed(2) + ' €) dépasse l\'estimation du client ('
                    + estime.toFixed(2) + ' €).\n' + message;
              }
              if (!confirm(message)) {
                e.preventDefault();
              }
            });
          })();
        </script>
      </c:when>
    </c:choose>
  </c:if>

  <p><a href="${pageContext.request.contextPath}/agent/sinistres">Retour aux sinistres</a></p>
</body>
</html>
