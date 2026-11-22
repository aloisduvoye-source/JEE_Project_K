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
    <a href="#garanties">Mes garanties</a> |
    <a href="#paiements">Mes paiements</a> |
    <a href="#sinistres">Mes sinistres</a> |
    <a href="#assistant">Assistant</a>
  </nav>

  <section id="contrats">
    <h2>Mes contrats</h2>
    <p><a href="${pageContext.request.contextPath}/client/contrats">Voir mes contrats et leurs garanties</a></p>
  </section>

  <section id="garanties">
    <h2>Mes garanties</h2>
    <p><a href="${pageContext.request.contextPath}/client/garanties">Voir mes garanties</a></p>
  </section>

  <section id="paiements">
    <h2>Mes paiements</h2>
    <ul>
      <li><a href="${pageContext.request.contextPath}/client/paiements">Consulter mes paiements et échéances</a></li>
      <li><a href="${pageContext.request.contextPath}/client/paiements?statut=EN_ATTENTE">Paiements en attente</a></li>
      <li><a href="${pageContext.request.contextPath}/client/paiements?statut=EFFECTUE">Historique des paiements effectués</a></li>
    </ul>
  </section>

  <section id="sinistres">
    <h2>Mes sinistres</h2>
    <ul>
      <li><a href="${pageContext.request.contextPath}/client/sinistres">Suivre mes sinistres</a></li>
      <li><a href="${pageContext.request.contextPath}/client/sinistres/nouveau">Déclarer un sinistre</a></li>
    </ul>
  </section>

  <section id="assistant">
    <h2>Assistant</h2>
    <p>Posez vos questions sur vos contrats, vos garanties ou vos sinistres. Par exemple :</p>
    <ul>
      <li>Quels sont mes contrats actifs ?</li>
      <li>Quelles garanties possède mon assurance automobile ?</li>
      <li>Comment déclarer un sinistre ?</li>
      <li>Où en est mon dernier sinistre ?</li>
    </ul>
    <p><a href="${pageContext.request.contextPath}/client/assistant">Discuter avec l'assistant</a></p>
  </section>
</body>
</html>
