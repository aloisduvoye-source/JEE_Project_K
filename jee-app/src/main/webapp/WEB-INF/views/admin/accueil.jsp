<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Espace admin</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Espace administrateur</h1>

  <%-- Onglets : liens vers les sections de cette page (à remplacer par des pages /admin/... plus tard) --%>
  <nav>
    <a href="#utilisateurs">Utilisateurs</a> |
    <a href="#types">Types d'assurance</a> |
    <a href="#contrats">Contrats</a> |
    <a href="#sinistres">Sinistres</a> |
    <a href="#statistiques">Statistiques</a> |
    <a href="#indicateurs">Indicateurs</a>
  </nav>

  <section id="utilisateurs">
    <h2>Utilisateurs</h2>
    <table border="1">
      <thead>
        <tr>
          <th>Nom</th>
          <th>Prénom</th>
          <th>Email</th>
          <th>Identifiant</th>
          <th>Rôle</th>
          <th>Actif</th>
          <th>Inscription</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td colspan="7">Aucun utilisateur à afficher pour l'instant.</td>
        </tr>
      </tbody>
    </table>
  </section>

  <section id="types">
    <h2>Types d'assurance</h2>
    <table border="1">
      <thead>
        <tr>
          <th>Nom</th>
          <th>Description</th>
          <th>Prix de base</th>
          <th>Garanties disponibles</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td colspan="4">Aucun type d'assurance à afficher pour l'instant.</td>
        </tr>
      </tbody>
    </table>
  </section>

  <section id="contrats">
    <h2>Contrats</h2>
    <ul>
      <li><a href="${pageContext.request.contextPath}/agent/contrats">Voir tous les contrats</a></li>
    </ul>
  </section>

  <section id="sinistres">
    <h2>Sinistres</h2>
    <table border="1">
      <thead>
        <tr>
          <th>Numéro</th>
          <th>Client</th>
          <th>Contrat</th>
          <th>Date</th>
          <th>Type</th>
          <th>Montant estimé</th>
          <th>Indemnisation</th>
          <th>Agent</th>
          <th>Statut</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td colspan="9">Aucun sinistre à afficher pour l'instant.</td>
        </tr>
      </tbody>
    </table>
  </section>

  <section id="statistiques">
    <h2>Statistiques globales</h2>

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

  <section id="indicateurs">
    <h2>Indicateurs de performance</h2>
    <ul>
      <li>Ratio indemnisations / primes encaissées : –</li>
      <li>Délai moyen de traitement d'un sinistre : –</li>
      <li>Taux d'acceptation des sinistres : –</li>
      <li>Taux de résiliation des contrats : –</li>
      <li>Paiements en retard : –</li>
    </ul>
  </section>
</body>
</html>
