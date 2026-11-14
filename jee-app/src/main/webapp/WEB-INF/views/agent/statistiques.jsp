<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Statistiques</title>
  <%-- Chargement de Chart.js pour les graphiques (ajouté avec l'aide de l'IA).
       Version fixée : sans elle, le CDN sert toujours la dernière, qui peut changer et casser la page --%>
  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Statistiques et analyse des données</h1>

  <section id="analyse" style="background: #f4f6fb; padding: 15px; border-radius: 8px; margin-bottom: 20px;">
    <h2>Analyse intelligente (Assistant IA Gemini)</h2>
    <%-- POST : recharger la page ne relance pas l'appel à l'API --%>
    <form method="post" action="${pageContext.request.contextPath}/agent/statistiques">
      <button type="submit">Générer une synthèse IA des statistiques</button>
    </form>
    <p>Une même synthèse est réutilisée pendant 10 minutes.</p>

    <%-- Synthèse gardée en session par StatistiquesServlet.doPost : affichée une fois puis effacée --%>
    <c:if test="${not empty sessionScope.analyseIa}">
      <div style="margin-top: 12px; white-space: pre-line; background: white; padding: 12px; border-left: 4px solid #2563eb;">
        <c:out value="${sessionScope.analyseIa}"/>
      </div>
      <c:remove var="analyseIa" scope="session"/>
    </c:if>
  </section>

  <section>
    <h2>Statistiques Clients</h2>
    <ul>
      <li>Total des clients : <strong><c:out value="${totalClients}"/></strong></li>
      <li>Nouveaux clients (ce mois-ci) : <strong><c:out value="${nouveauxClients}"/></strong></li>
    </ul>

    <h3>Répartition par type d'assurance</h3>
    <table border="1" id="tableClients">
      <thead>
        <tr>
          <th>Type d'assurance</th>
          <th>Clients ayant un contrat actif</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="ligne" items="${repartitionClients}">
          <tr data-libelle="<c:out value='${ligne[0]}'/>" data-valeur="<c:out value='${ligne[1]}'/>">
            <td><c:out value="${ligne[0]}"/></td>
            <td><c:out value="${ligne[1]}"/></td>
          </tr>
        </c:forEach>
        <c:if test="${empty repartitionClients}">
          <tr><td colspan="2">Aucune donnée disponible.</td></tr>
        </c:if>
      </tbody>
    </table>

    <%-- Zone d'affichage du camembert Chart.js (généré avec l'aide de l'IA) --%>
    <div style="width: 400px; margin-top: 15px;">
      <canvas id="chartClients"></canvas>
    </div>
  </section>

  <section>
    <h2>Statistiques Contrats</h2>
    <ul>
      <li>Contrats actifs : <strong><c:out value="${contratsActifs}"/></strong></li>
      <li>Contrats expirés : <strong><c:out value="${contratsExpires}"/></strong></li>
      <li>Contrats résiliés : <strong><c:out value="${contratsResilies}"/></strong></li>
    </ul>

    <h3>Évolution du nombre de contrats actifs</h3>
    <p>Contrats actifs en même temps, en fin de mois (aujourd'hui pour le mois en cours).</p>
    <table border="1" id="tableContrats">
      <thead>
        <tr>
          <th>Période (Mois / Année)</th>
          <th>Contrats actifs</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="ligne" items="${evolutionContrats}">
          <tr data-libelle="<c:out value='${ligne[0]}'/>" data-valeur="<c:out value='${ligne[1]}'/>">
            <td><c:out value="${ligne[0]}"/></td>
            <td><c:out value="${ligne[1]}"/></td>
          </tr>
        </c:forEach>
        <c:if test="${empty evolutionContrats}">
          <tr><td colspan="2">Aucun contrat enregistré.</td></tr>
        </c:if>
      </tbody>
    </table>

    <%-- Zone d'affichage de la courbe Chart.js (généré avec l'aide de l'IA) --%>
    <div style="width: 550px; margin-top: 15px;">
      <canvas id="chartContrats"></canvas>
    </div>
  </section>

  <section>
    <h2>Statistiques Financières</h2>
    <ul>
      <li>Chiffre d'affaires encaissé (paiements effectués, avant remboursements) : <strong><c:out value="${chiffreAffaires}"/> €</strong></li>
      <li>Montant total des remboursements (résiliations) : <strong><c:out value="${totalRemboursements}"/> €</strong></li>
      <li>Chiffre d'affaires net (encaissé - remboursements) : <strong><c:out value="${chiffreAffairesNet}"/> €</strong></li>
      <li>Montant total des primes actives : <strong><c:out value="${totalPrimes}"/> €</strong></li>
      <li>Montant total des indemnisations : <strong><c:out value="${totalIndemnisations}"/> €</strong></li>
      <li>Ratio indemnisations / primes encaissées (chiffre d'affaires net) :
        <strong>
          <c:choose>
            <c:when test="${empty ratioIndemnisations}">–</c:when>
            <c:otherwise><c:out value="${ratioIndemnisations}"/> %</c:otherwise>
          </c:choose>
        </strong>
      </li>
    </ul>

    <h3>Évolution mensuelle des flux financiers</h3>
    <p>Argent encaissé (paiements effectués) et versé aux clients (indemnisations, remboursements), mois par mois.</p>
    <table border="1" id="tableRevenus">
      <thead>
        <tr>
          <th>Période (Mois / Année)</th>
          <th>Encaissé (€)</th>
          <th>Indemnisations (€)</th>
          <th>Remboursements (€)</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="ligne" items="${evolutionFinances}">
          <tr data-libelle="<c:out value='${ligne[0]}'/>" data-encaisse="<c:out value='${ligne[1]}'/>"
              data-indemnise="<c:out value='${ligne[2]}'/>" data-rembourse="<c:out value='${ligne[3]}'/>">
            <td><c:out value="${ligne[0]}"/></td>
            <td><c:out value="${ligne[1]}"/> €</td>
            <td><c:out value="${ligne[2]}"/> €</td>
            <td><c:out value="${ligne[3]}"/> €</td>
          </tr>
        </c:forEach>
        <c:if test="${empty evolutionFinances}">
          <tr><td colspan="4">Aucun paiement ni versement.</td></tr>
        </c:if>
      </tbody>
    </table>

    <%-- Zone d'affichage de l'histogramme Chart.js (généré avec l'aide de l'IA) --%>
    <div style="width: 550px; margin-top: 15px;">
      <canvas id="chartRevenus"></canvas>
    </div>
  </section>

  <%-- Page partagée agent / admin : retour vers l'espace du rôle connecté --%>
  <p><a href="${pageContext.request.contextPath}${sessionScope.utilisateur.role.accueil}">Retour à mon espace</a></p>

  <%-- Script Chart.js pour dessiner les 3 graphiques (généré avec l'aide de l'IA) --%>
  <script>
    // Libellés et valeurs lus dans les lignes du tableau (attributs data-) : c:out dans un attribut HTML
    // est décodé par le navigateur, alors que dans le script une apostrophe resterait affichée "&#039;"
    // attributs : noms des valeurs à lire (data-valeur par défaut), une liste de nombres par nom
    function donnees(idTableau, attributs = ['valeur']) {
      const lignes = document.querySelectorAll('#' + idTableau + ' tbody tr[data-libelle]');
      const resultat = {libelles: Array.from(lignes, ligne => ligne.dataset.libelle)};
      for (const attribut of attributs) {
        resultat[attribut] = Array.from(lignes, ligne => Number(ligne.dataset[attribut]));
      }
      return resultat;
    }

    // 1. Camembert : répartition des clients par type d'assurance (aide de l'ia)
    const clients = donnees('tableClients');
    new Chart(document.getElementById('chartClients'), {
      type: 'pie',
      data: {
        labels: clients.libelles,
        datasets: [{
          data: clients.valeur
        }]
      }
    });

    // 2. Courbe : nombre de contrats actifs en même temps, mois par mois (aide de l'ia)
    const contrats = donnees('tableContrats');
    new Chart(document.getElementById('chartContrats'), {
      type: 'line',
      data: {
        labels: contrats.libelles,
        datasets: [{
          label: 'Contrats actifs',
          data: contrats.valeur
        }]
      }
    });

    // 3. Barres : argent encaissé et versé par mois (aide de l'ia)
    const finances = donnees('tableRevenus', ['encaisse', 'indemnise', 'rembourse']);
    new Chart(document.getElementById('chartRevenus'), {
      type: 'bar',
      data: {
        labels: finances.libelles,
        datasets: [
          {label: 'Encaissé (€)', data: finances.encaisse},
          {label: 'Indemnisations (€)', data: finances.indemnise},
          {label: 'Remboursements (€)', data: finances.rembourse}
        ]
      }
    });
  </script>
</body>
</html>
