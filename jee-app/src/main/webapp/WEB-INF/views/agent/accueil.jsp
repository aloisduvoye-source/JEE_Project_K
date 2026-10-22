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
    <a href="#clients">Clients</a>
  </nav>

  <section id="clients">
    <h2>Clients</h2>
    <table border="1">
      <thead>
        <tr>
          <th>Nom</th>
          <th>Prénom</th>
          <th>Email</th>
          <th>Téléphone</th>
          <th>Contrats</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td colspan="5">Aucun client à afficher pour l'instant.</td>
        </tr>
      </tbody>
    </table>
  </section>
</body>
</html>
