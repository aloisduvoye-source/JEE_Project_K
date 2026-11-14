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

  <section>
    <h2>Statistiques Clients</h2>
    <ul>
      <li>Total des clients : <strong><c:out value="${totalClients}"/></strong></li>
      <li>Nouveaux clients (ce mois-ci) : <strong><c:out value="${nouveauxClients}"/></strong></li>
    </ul>

    <h3>Répartition par type d'assurance</h3>
    <table border="1">
      <thead>
        <tr>
          <th>Type d'assurance</th>
          <th>Nombre de clients</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="ligne" items="${repartitionClients}">
          <tr>
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

    <h3>Évolution du nombre de contrats</h3>
    <table border="1">
      <thead>
        <tr>
          <th>Période (Mois / Année)</th>
          <th>Nombre de contrats</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="ligne" items="${evolutionContrats}">
          <tr>
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
      <li>Chiffre d'affaires encaissé : <strong><c:out value="${chiffreAffaires}"/> €</strong></li>
      <li>Montant total des primes actives : <strong><c:out value="${totalPrimes}"/> €</strong></li>
      <li>Montant total des indemnisations : <strong><c:out value="${totalIndemnisations}"/> €</strong></li>
    </ul>

    <h3>Évolution des revenus</h3>
    <table border="1">
      <thead>
        <tr>
          <th>Période (Mois / Année)</th>
          <th>Revenus encaissés (€)</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="ligne" items="${evolutionRevenus}">
          <tr>
            <td><c:out value="${ligne[0]}"/></td>
            <td><c:out value="${ligne[1]}"/> €</td>
          </tr>
        </c:forEach>
        <c:if test="${empty evolutionRevenus}">
          <tr><td colspan="2">Aucun paiement effectué.</td></tr>
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
    // 1. Camembert : répartition des clients par type d'assurance (aide de l'ia)
    new Chart(document.getElementById('chartClients'), {
      type: 'pie',
      data: {
        labels: [<c:forEach var="l" items="${repartitionClients}">'<c:out value="${l[0]}"/>',</c:forEach>],
        datasets: [{
          data: [<c:forEach var="l" items="${repartitionClients}">${l[1]},</c:forEach>]
        }]
      }
    });

    // 2. Courbe : évolution du nombre de contrats par mois (aide de l'ia)
    new Chart(document.getElementById('chartContrats'), {
      type: 'line',
      data: {
        labels: [<c:forEach var="l" items="${evolutionContrats}">'${l[0]}',</c:forEach>],
        datasets: [{
          label: 'Nombre de contrats',
          data: [<c:forEach var="l" items="${evolutionContrats}">${l[1]},</c:forEach>]
        }]
      }
    });

    // 3. Barres : évolution des revenus par mois (aide de l'ia)
    new Chart(document.getElementById('chartRevenus'), {
      type: 'bar',
      data: {
        labels: [<c:forEach var="l" items="${evolutionRevenus}">'${l[0]}',</c:forEach>],
        datasets: [{
          label: 'Revenus (€)',
          data: [<c:forEach var="l" items="${evolutionRevenus}">${l[1]},</c:forEach>]
        }]
      }
    });
  </script>
</body>
</html>