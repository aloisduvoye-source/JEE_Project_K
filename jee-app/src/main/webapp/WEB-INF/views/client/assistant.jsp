<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Assistant</title>
  <style>
    .conversation { max-width: 800px; }
    .message { margin: 0.5em 0; padding: 0.5em 0.8em; border-radius: 6px; }
    .message.client { background: #e3edff; margin-left: 15%; }
    .message.assistant { background: #f0f0f0; margin-right: 15%; }
    /* Les retours à la ligne de la réponse sont conservés ; c:out empêche toute injection de HTML */
    .message p { margin: 0.3em 0 0; white-space: pre-wrap; }
    .message small { color: #666; }
    textarea { width: 100%; max-width: 800px; box-sizing: border-box; }
  </style>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <h1>Assistant</h1>
  <p>
    Posez vos questions sur vos contrats, vos garanties, vos paiements ou vos sinistres.
    L'assistant répond à partir des informations de votre espace ; il ne peut effectuer aucune opération à votre place.
  </p>

  <div class="conversation">
    <c:if test="${empty messages}">
      <p>Aucune question pour l'instant. Quelques exemples :</p>
      <%-- Un clic envoie directement la question (formulaire en POST, comme le champ ci-dessous) --%>
      <c:forEach var="exemple" items="${[
          'Quels sont mes contrats actifs ?',
          'Quelles garanties possède mon assurance automobile ?',
          'Comment déclarer un sinistre ?',
          'Quels documents dois-je fournir pour mon sinistre ?',
          'Quelle est la différence entre la garantie vol et la garantie accident ?',
          'Où en est mon dernier sinistre ?']}">
        <form method="post" action="${pageContext.request.contextPath}/client/assistant" class="form-question">
          <input type="hidden" name="question" value="${exemple}">
          <button type="submit"><c:out value="${exemple}"/></button>
        </form>
      </c:forEach>
    </c:if>

    <c:forEach var="m" items="${messages}">
      <div class="message ${m.auteur == 'CLIENT' ? 'client' : 'assistant'}">
        <small>
          <c:out value="${m.auteur == 'CLIENT' ? 'Vous' : 'Assistant'}"/>,
          <c:out value="${m.dateMessage.toLocalDate()} à ${m.dateMessage.toLocalTime().withNano(0).withSecond(0)}"/>
        </small>
        <p><c:out value="${m.contenu}"/></p>
      </div>
    </c:forEach>
  </div>

  <div id="bas"></div>

  <%-- Erreur enregistrée avant la redirection (MessageFlash) : question vide, assistant indisponible... --%>
  <c:if test="${not empty sessionScope.messageErreur}">
    <p><strong><c:out value="${sessionScope.messageErreur}"/></strong></p>
    <c:remove var="messageErreur" scope="session"/>
  </c:if>

  <form method="post" action="${pageContext.request.contextPath}/client/assistant" class="form-question">
    <p>
      <label for="question">Votre question</label><br>
      <%-- Question remise dans le champ si l'assistant n'a pas pu répondre --%>
      <textarea id="question" name="question" rows="3" maxlength="${longueurMax}" required
                autofocus><c:out value="${sessionScope.questionNonEnvoyee}"/></textarea>
      <c:remove var="questionNonEnvoyee" scope="session"/>
    </p>
    <button type="submit">Envoyer</button>
    <span class="attente" hidden>L'assistant réfléchit, cela peut prendre quelques secondes…</span>
  </form>

  <c:if test="${not empty messages}">
    <form method="post" action="${pageContext.request.contextPath}/client/assistant"
          onsubmit="return confirm('Effacer toute la conversation ?');">
      <input type="hidden" name="action" value="effacer">
      <p><button type="submit">Effacer la conversation</button></p>
    </form>
  </c:if>

  <p><a href="${pageContext.request.contextPath}/client/accueil">Retour à mon espace</a></p>

  <script>
    // La réponse prend quelques secondes : on empêche un double envoi et on prévient le client
    document.querySelectorAll('.form-question').forEach(function (formulaire) {
      formulaire.addEventListener('submit', function () {
        document.querySelectorAll('.form-question button').forEach(function (bouton) {
          bouton.disabled = true;
        });
        document.querySelector('.attente').hidden = false;
      });
    });

    // Entrée envoie la question, Maj + Entrée passe à la ligne
    document.getElementById('question').addEventListener('keydown', function (e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        if (this.value.trim() !== '') {
          this.form.requestSubmit();
        }
      }
    });
  </script>
</body>
</html>
