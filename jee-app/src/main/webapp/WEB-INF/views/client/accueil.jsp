<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Espace client</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Espace client</h1>

  <%-- Onglets : liens vers les sections de cette page (à remplacer par des pages /client/... plus tard) --%>
  <nav>
    <a href="#contrats">Mes contrats</a> |
    <a href="#garanties">Mes garanties</a>
  </nav>

  <section id="contrats">
    <h2>Mes contrats</h2>
    <table border="1">
      <thead>
        <tr>
          <th>Numéro</th>
          <th>Type d'assurance</th>
          <th>Début</th>
          <th>Fin</th>
          <th>Prime</th>
          <th>Statut</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td colspan="6">Vous n'avez aucun contrat pour l'instant.</td>
        </tr>
      </tbody>
    </table>
  </section>

  <section id="garanties">
    <h2>Mes garanties</h2>
    <table border="1">
      <thead>
        <tr>
          <th>Contrat</th>
          <th>Garantie</th>
          <th>Description</th>
          <th>Plafond</th>
          <th>Franchise</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td colspan="5">Aucune garantie à afficher pour l'instant.</td>
        </tr>
      </tbody>
    </table>
  </section>
</body>
</html>
