<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Assistant · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur conteneur-etroit apparition">
      <div class="entete-page">
        <div>
          <h1>Assistant</h1>
          <p class="sous-titre">
            Vos questions sur vos contrats, garanties, paiements et sinistres. L'assistant répond à partir
            des informations de votre espace ; il ne peut effectuer aucune opération à votre place.
          </p>
        </div>
        <%-- Toujours présent, masqué tant que la conversation est vide : il apparaît après la première réponse --%>
        <form class="actions" method="post" action="${pageContext.request.contextPath}/client/assistant" id="form-effacer"
              ${empty messages ? 'hidden' : ''}
              data-confirmer="Effacer la conversation ?"
              data-confirmer-texte="Tous les messages seront supprimés : l'assistant repartira de zéro."
              data-confirmer-bouton="Effacer">
          <input type="hidden" name="action" value="effacer">
          <button type="submit" class="bouton bouton-fantome">
            <svg class="icone" aria-hidden="true"><use href="#i-poubelle"/></svg>Effacer la conversation
          </button>
        </form>
      </div>

      <section class="carte">
        <c:if test="${empty messages}">
          <div class="vide" id="exemples">
            <span class="pastille ton-violet"><svg class="icone" aria-hidden="true"><use href="#i-etincelles"/></svg></span>
            <h2>Comment puis-je vous aider ?</h2>
            <p>Choisissez une question ou écrivez la vôtre ci-dessous.</p>
            <%-- Un clic envoie directement la question (formulaire en POST, comme le champ ci-dessous) --%>
            <div class="suggestions" style="justify-content:center;margin-top:8px">
              <c:forEach var="exemple" items="${[
                  'Quels sont mes contrats actifs ?',
                  'Quelles garanties possède mon assurance automobile ?',
                  'Comment déclarer un sinistre ?',
                  'Quels documents dois-je fournir pour mon sinistre ?',
                  'Quelle est la différence entre la garantie vol et la garantie accident ?',
                  'Où en est mon dernier sinistre ?']}">
                <form method="post" action="${pageContext.request.contextPath}/client/assistant" class="form-question">
                  <input type="hidden" name="question" value="${exemple}">
                  <button type="submit" class="suggestion" style="cursor:pointer">
                    <svg class="icone" aria-hidden="true"><use href="#i-message"/></svg><c:out value="${exemple}"/>
                  </button>
                </form>
              </c:forEach>
            </div>
          </div>
        </c:if>
        <div class="fil" id="conversation" ${empty messages ? 'hidden' : ''}>
          <c:forEach var="m" items="${messages}">
            <div class="bulle ${m.auteur == 'CLIENT' ? 'bulle-moi' : ''}">
              <div class="bulle-auteur">
                <strong>${m.auteur == 'CLIENT' ? 'Vous' : 'Assistant'}</strong>
                <span><app:dateHeure valeur="${m.dateMessage}"/></span>
              </div>
              <%-- c:out empêche toute injection de HTML ; bulle-texte garde les retours à la ligne --%>
              <p class="bulle-texte"><c:out value="${m.contenu}"/></p>
            </div>
          </c:forEach>
        </div>

        <form class="fil-saisie form-question" method="post" action="${pageContext.request.contextPath}/client/assistant">
          <%-- Erreur enregistrée avant la redirection (MessageFlash) : question vide, assistant indisponible... --%>
          <c:if test="${not empty sessionScope.messageErreur}">
            <div class="alerte ton-danger" role="alert">
              <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
              <div class="alerte-texte"><c:out value="${sessionScope.messageErreur}"/></div>
            </div>
            <c:remove var="messageErreur" scope="session"/>
          </c:if>
          <%-- Erreur renvoyée en JSON quand la question est envoyée sans recharger la page --%>
          <div class="alerte ton-danger" role="alert" id="erreur-assistant" hidden>
            <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
            <div class="alerte-texte"></div>
          </div>
          <div class="champ">
            <label class="visuellement-cache" for="question">Votre question</label>
            <%-- Question remise dans le champ si l'assistant n'a pas pu répondre --%>
            <textarea class="saisie" id="question" name="question" rows="2" maxlength="${longueurMax}" required autofocus
                      data-compteur placeholder="Posez votre question… (Entrée pour envoyer, Maj + Entrée pour aller à la ligne)"><c:out value="${sessionScope.questionNonEnvoyee}"/></textarea>
            <c:remove var="questionNonEnvoyee" scope="session"/>
          </div>
          <div class="actions-formulaire">
            <span class="fil-attente" hidden>
              <svg class="icone" aria-hidden="true"><use href="#i-horloge"/></svg>L'assistant réfléchit, cela peut prendre quelques secondes…
            </span>
            <button type="submit" class="bouton bouton-principal">
              <svg class="icone" aria-hidden="true"><use href="#i-message"/></svg>Envoyer
            </button>
          </div>
        </form>
      </section>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>

  <script>
    // La question est envoyée sans recharger la page : la réponse est ajoutée à la conversation.
    // Session expirée : le serveur redirige vers la page de connexion au lieu de répondre en JSON,
    // on y emmène le client.
    var conversation = document.getElementById('conversation');
    var champ = document.getElementById('question');
    var attente = document.querySelector('.fil-attente');
    var erreur = document.getElementById('erreur-assistant');
    var MOIS = ['janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin', 'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.'];

    function deuxChiffres(n) {
      return (n < 10 ? '0' : '') + n;
    }

    // Même affichage que les messages générés par la JSP (app:dateHeure) : "13 nov. 2026 à 15:48"
    function formaterDate(d) {
      return d.getDate() + '\u00a0' + MOIS[d.getMonth()] + '\u00a0' + d.getFullYear()
          + '\u00a0à\u00a0' + deuxChiffres(d.getHours()) + ':' + deuxChiffres(d.getMinutes());
    }

    // Date envoyée par le serveur en JSON : "2026-11-13 à 15:48"
    function lireDate(texte) {
      var m = /^(\d{4})-(\d{2})-(\d{2}) à (\d{2}):(\d{2})$/.exec(texte || '');
      return m ? formaterDate(new Date(m[1], m[2] - 1, m[3], m[4], m[5])) : (texte || '');
    }

    // textContent et non innerHTML : le texte de la réponse ne peut pas injecter de HTML
    function ajouterMessage(auteur, contenu, date) {
      var bulle = document.createElement('div');
      bulle.className = 'bulle' + (auteur === 'CLIENT' ? ' bulle-moi' : '');
      var entete = document.createElement('div');
      entete.className = 'bulle-auteur';
      var nom = document.createElement('strong');
      nom.textContent = auteur === 'CLIENT' ? 'Vous' : 'Assistant';
      var quand = document.createElement('span');
      quand.textContent = date;
      entete.append(nom, quand);
      var texte = document.createElement('p');
      texte.className = 'bulle-texte';
      texte.textContent = contenu;
      bulle.append(entete, texte);
      conversation.hidden = false;
      conversation.appendChild(bulle);
      bulle.scrollIntoView({block: 'end'});
      return bulle;
    }

    function patienter(enCours) {
      document.querySelectorAll('.form-question button').forEach(function (bouton) {
        bouton.disabled = enCours;
      });
      attente.hidden = !enCours;
    }

    function afficherErreur(texte) {
      erreur.querySelector('.alerte-texte').textContent = texte;
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
        var bulle = ajouterMessage('CLIENT', question, formaterDate(new Date()));
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
              ajouterMessage(json.auteur, json.contenu, lireDate(json.date));
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
