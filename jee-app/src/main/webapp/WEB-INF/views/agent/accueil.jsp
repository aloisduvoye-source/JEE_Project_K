<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Espace agent</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Espace agent</h1>

  <%-- Onglets : liens vers les sections de cette page (à remplacer par des pages /agent/... plus tard) --%>
  <nav>
    <a href="#clients">Clients</a> |
    <a href="#contrats">Contrats</a> |
    <a href="#sinistres">Sinistres</a> |
    <a href="#paiements">Paiements</a> |
    <a href="#garanties">Garanties</a> |
    <a href="#statistiques">Statistiques</a>
  </nav>

  <section id="clients">
    <h2>Clients</h2>
    <ul>
      <li><a href="${pageContext.request.contextPath}/agent/clients">Voir tous les clients</a></li>
    </ul>
  </section>

  <section id="contrats">
    <h2>Contrats</h2>
    <ul>
      <li><a href="${pageContext.request.contextPath}/agent/contrats">Voir tous les contrats</a></li>
      <li><a href="${pageContext.request.contextPath}/agent/contrats/nouveau">Nouveau contrat</a></li>
    </ul>
  </section>

  <section id="sinistres">
    <h2>Sinistres</h2>
    <ul>
      <li><a href="${pageContext.request.contextPath}/agent/sinistres?statut=DECLARE">Sinistres à prendre en charge</a></li>
      <li><a href="${pageContext.request.contextPath}/agent/sinistres?mien=1">Mes dossiers</a></li>
      <li><a href="${pageContext.request.contextPath}/agent/sinistres">Tous les sinistres</a></li>
    </ul>
  </section>

  <section id="paiements">
    <h2>Paiements</h2>
    <table border="1">
      <thead>
        <tr>
          <th>Contrat</th>
          <th>Client</th>
          <th>Échéance</th>
          <th>Montant</th>
          <th>Date de paiement</th>
          <th>Mode</th>
          <th>Statut</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td colspan="7">Aucun paiement à afficher pour l'instant.</td>
        </tr>
      </tbody>
    </table>
  </section>

  <section id="garanties">
    <h2>Garanties</h2>
    <ul>
      <li><a href="${pageContext.request.contextPath}/agent/garanties">Gérer les garanties</a></li>
    </ul>
  </section>

  <section id="statistiques">
    <h2>Statistiques</h2>

    <h3>Clients</h3>
    <ul>
      <li>Nombre total de clients : –</li>
      <li>Nouveaux clients : –</li>
      <li>Répartition par type d'assurance : –</li>
    </ul>

    <h3>Contrats</h3>
    <ul>
      <li>Contrats actifs : –</li>
      <li>Contrats expirés : –</li>
      <li>Contrats résiliés : –</li>
      <li>Évolution du nombre de contrats : –</li>
    </ul>

    <h3>Finances</h3>
    <ul>
      <li>Chiffre d'affaires : –</li>
      <li>Montant des primes : –</li>
      <li>Montant des indemnisations : –</li>
      <li>Évolution des revenus : –</li>
    </ul>
  </section>
</body>
</html>
