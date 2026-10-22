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
    <a href="#utilisateurs">Utilisateurs</a>
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
</body>
</html>
