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

  <div class="conversation" id="conversation">
    <c:if test="${empty messages}">
      <div id="exemples">
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
      </div>
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
  <%-- Erreur renvoyée en JSON quand la question est envoyée sans recharger la page --%>
  <p id="erreur-assistant" hidden><strong></strong></p>

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

  <%-- Toujours présent, masqué tant que la conversation est vide : il apparaît après la première réponse --%>
  <form method="post" action="${pageContext.request.contextPath}/client/assistant" id="form-effacer"
        onsubmit="return confirm('Effacer toute la conversation ?');" ${empty messages ? 'hidden' : ''}>
    <input type="hidden" name="action" value="effacer">
    <p><button type="submit">Effacer la conversation</button></p>
  </form>

  <p><a href="${pageContext.request.contextPath}/client/accueil">Retour à mon espace</a></p>

  <script>
    // La question est envoyée sans recharger la page : la réponse est ajoutée à la conversation.
    // Session expirée : le serveur redirige vers la page de connexion au lieu de répondre en JSON,
    // on y emmène le client.
    var conversation = document.getElementById('conversation');
    var champ = document.getElementById('question');
    var attente = document.querySelector('.attente');
    var erreur = document.getElementById('erreur-assistant');

    function deuxChiffres(n) {
      return (n < 10 ? '0' : '') + n;
    }

    // Même affichage que les messages générés par la JSP : "2026-11-13 à 15:48"
    function maintenant() {
      var d = new Date();
      return d.getFullYear() + '-' + deuxChiffres(d.getMonth() + 1) + '-' + deuxChiffres(d.getDate())
          + ' à ' + deuxChiffres(d.getHours()) + ':' + deuxChiffres(d.getMinutes());
    }

    // textContent et non innerHTML : le texte de la réponse ne peut pas injecter de HTML
    function ajouterMessage(auteur, contenu, date) {
      var message = document.createElement('div');
      message.className = 'message ' + (auteur === 'CLIENT' ? 'client' : 'assistant');
      var entete = document.createElement('small');
      entete.textContent = (auteur === 'CLIENT' ? 'Vous' : 'Assistant') + ', ' + date;
      var texte = document.createElement('p');
      texte.textContent = contenu;
      message.appendChild(entete);
      message.appendChild(texte);
      conversation.appendChild(message);
      message.scrollIntoView({block: 'end'});
      return message;
    }

    function patienter(enCours) {
      document.querySelectorAll('.form-question button').forEach(function (bouton) {
        bouton.disabled = enCours;
      });
      attente.hidden = !enCours;
    }

    function afficherErreur(texte) {
      erreur.querySelector('strong').textContent = texte;
      erreur.hidden = false;
    }

    document.querySelectorAll('.form-question').forEach(function (formulaire) {
      formulaire.addEventListener('submit', function (e) {
        e.preventDefault();
        var donnees = new FormData(formulaire);
        var question = (donnees.get('question') || '').trim();
        if (question === '') {
          return;
        }

        erreur.hidden = true;
        patienter(true);
        // La question s'affiche tout de suite, la réponse arrive quelques secondes plus tard
        var bulle = ajouterMessage('CLIENT', question, maintenant());
        var exemples = document.getElementById('exemples');
        if (exemples) {
          exemples.remove();
        }
        if (formulaire.contains(champ)) {
          champ.value = '';
        }

        fetch(formulaire.action, {
          method: 'POST',
          headers: {'Accept': 'application/json'},
          body: new URLSearchParams(donnees)
        }).then(function (reponse) {
          var type = reponse.headers.get('Content-Type') || '';
          if (type.indexOf('application/json') === -1) {
            if (reponse.redirected) {
              window.location.href = reponse.url;
              return;
            }
            // Page d'erreur du serveur (HTML) : rien n'a été enregistré
            throw new Error('Réponse inattendue');
          }
          return reponse.json().then(function (json) {
            if (json.erreur) {
              // Rien n'a été enregistré : on retire la question et on la remet dans le champ
              bulle.remove();
              champ.value = question;
              afficherErreur(json.erreur);
            } else {
              ajouterMessage(json.auteur, json.contenu, json.date);
              document.getElementById('form-effacer').hidden = false;
            }
          });
        }).catch(function () {
          bulle.remove();
          champ.value = question;
          afficherErreur("L'assistant n'a pas pu répondre. Vérifiez votre connexion puis réessayez.");
        }).finally(function () {
          patienter(false);
          champ.focus();
        });
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
