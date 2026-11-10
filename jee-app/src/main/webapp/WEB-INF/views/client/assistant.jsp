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
        <c:if test="${not empty messages}">
          <form class="actions" method="post" action="${pageContext.request.contextPath}/client/assistant"
                data-confirmer="Effacer la conversation ?"
                data-confirmer-texte="Tous les messages seront supprimés : l'assistant repartira de zéro."
                data-confirmer-bouton="Effacer">
            <input type="hidden" name="action" value="effacer">
            <button type="submit" class="bouton bouton-fantome">
              <svg class="icone" aria-hidden="true"><use href="#i-poubelle"/></svg>Effacer la conversation
            </button>
          </form>
        </c:if>
      </div>

      <section class="carte">
        <c:choose>
          <c:when test="${empty messages}">
            <div class="vide">
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
          </c:when>
          <c:otherwise>
            <div class="fil">
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
          </c:otherwise>
        </c:choose>

        <div id="bas"></div>

        <form class="fil-saisie form-question" method="post" action="${pageContext.request.contextPath}/client/assistant">
          <%-- Erreur enregistrée avant la redirection (MessageFlash) : question vide, assistant indisponible... --%>
          <c:if test="${not empty sessionScope.messageErreur}">
            <div class="alerte ton-danger" role="alert">
              <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
              <div class="alerte-texte"><c:out value="${sessionScope.messageErreur}"/></div>
            </div>
            <c:remove var="messageErreur" scope="session"/>
          </c:if>
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
    // La réponse prend quelques secondes : on empêche un double envoi et on prévient le client
    document.querySelectorAll('.form-question').forEach(function (formulaire) {
      formulaire.addEventListener('submit', function () {
        document.querySelectorAll('.form-question button').forEach(function (bouton) {
          bouton.disabled = true;
        });
        document.querySelector('.fil-attente').hidden = false;
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
