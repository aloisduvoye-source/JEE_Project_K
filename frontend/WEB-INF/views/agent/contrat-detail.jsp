<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Contrat n° <c:out value="${contrat.id}"/> · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div>
        <a class="retour" href="${pageContext.request.contextPath}/agent/contrats">
          <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Contrats
        </a>
        <div class="entete-page">
          <div>
            <h1>Contrat n° <c:out value="${contrat.id}"/></h1>
            <p class="sous-titre"><c:out value="${contrat.typeAssurance.nom}"/> · <c:out value="${contrat.client.utilisateur.prenom} ${contrat.client.utilisateur.nom}"/></p>
          </div>
          <%-- Modification par l'agent seulement, tant que le contrat n'est pas payé (ModifierContratServlet le vérifie aussi) --%>
          <c:if test="${sessionScope.utilisateur.role == 'AGENT'
                        and (contrat.statut == 'EN_ATTENTE' or contrat.statut == 'SUSPENDU')
                        and (empty paiement or paiement.statut != 'EFFECTUE')}">
            <div class="actions">
              <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/agent/contrats/modifier?id=${contrat.id}">
                <svg class="icone" aria-hidden="true"><use href="#i-crayon"/></svg>Modifier le contrat
              </a>
            </div>
          </c:if>
        </div>
      </div>

      <%@ include file="/WEB-INF/views/common/messages-contrat.jspf" %>

      <div class="disposition-laterale">
        <div class="pile">
          <%@ include file="/WEB-INF/views/common/contrat-infos.jspf" %>
        </div>
        <aside class="pile">
          <%-- Le paiement est affiché sans formulaire : c'est le client qui paie, depuis sa propre fiche --%>
          <%@ include file="/WEB-INF/views/common/contrat-paiement.jspf" %>

          <%-- Suspension par un agent seulement (ActionContratServlet et ContratService le vérifient aussi) --%>
          <c:if test="${sessionScope.utilisateur.role == 'AGENT' and contrat.statut == 'ACTIF'}">
            <section class="carte">
              <div class="carte-entete">
                <div><h2>Suspendre le contrat</h2><p>Pendant la suspension, le contrat ne couvre plus les sinistres. Elle dure jusqu'à ce qu'un agent la lève.</p></div>
              </div>
              <form class="carte-corps formulaire" method="post" action="${pageContext.request.contextPath}/agent/contrats/suspendre"
                    data-confirmer="Suspendre ce contrat ?"
                    data-confirmer-texte="Il ne couvrira plus les sinistres jusqu'à la levée de la suspension."
                    data-confirmer-bouton="Suspendre">
                <input type="hidden" name="id" value="${contrat.id}">
                <div class="champ">
                  <label for="motifSuspension">Motif <span class="texte-doux">(visible par le client)</span></label>
                  <input class="saisie" type="text" id="motifSuspension" name="motif" maxlength="255" required>
                </div>
                <div class="actions-formulaire">
                  <button type="submit" class="bouton bouton-danger-doux bouton-bloc">
                    <svg class="icone" aria-hidden="true"><use href="#i-cadenas"/></svg>Suspendre le contrat
                  </button>
                </div>
              </form>
            </section>
          </c:if>
          <c:if test="${sessionScope.utilisateur.role == 'AGENT' and contrat.suspenduParAgent}">
            <section class="carte">
              <div class="carte-entete">
                <div><h2>Lever la suspension</h2><p>Le contrat couvrira de nouveau les sinistres.</p></div>
              </div>
              <form class="carte-corps formulaire" method="post" action="${pageContext.request.contextPath}/agent/contrats/lever-suspension">
                <input type="hidden" name="id" value="${contrat.id}">
                <div class="actions-formulaire">
                  <button type="submit" class="bouton bouton-principal bouton-bloc">
                    <svg class="icone" aria-hidden="true"><use href="#i-restaurer"/></svg>Lever la suspension
                  </button>
                </div>
              </form>
            </section>
          </c:if>

          <%@ include file="/WEB-INF/views/common/contrat-renouvellement.jspf" %>
          <%@ include file="/WEB-INF/views/common/contrat-resiliation.jspf" %>
        </aside>
      </div>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
